package gov.cms.dpc.aggregation.bbclient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.typesafe.config.Config;
import org.apache.http.client.HttpClient;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.MissingResourceException;

public class BlueButtonClientModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(BlueButtonClientModule.class);
    // Used to retrieve the keystore from the JAR resources. This path is relative to the Resources root.
    private static final String KEYSTORE_RESOURCE_KEY = "/bb.keystore";

    public BlueButtonClientModule() {

    }

    @Override
    protected void configure() {
        //Unused
    }

    @Provides
    public BlueButtonClient provideBlueButtonClient(IGenericClient fhirRestClient) {
        return new DefaultBlueButtonClient(fhirRestClient);
    }

    @Provides
    public IGenericClient provideFhirRestClient(Config config, FhirContext fhirContext, HttpClient httpClient) {
        final String serverBaseUrl = config.getString("bbclient.serverBaseUrl");
        final IRestfulClientFactory factory = fhirContext.getRestfulClientFactory();
        factory.setHttpClient(httpClient);
        factory.setConnectionRequestTimeout(2000);
        factory.setSocketTimeout(2000);

        return fhirContext.newRestfulGenericClient(serverBaseUrl);
    }

    @Provides
    public KeyStore provideKeyStore(Config config) {
        final String keyStoreType = config.getString("bbclient.keyStore.type");
        final String defaultKeyStorePassword = config.getString("bbclient.keyStore.defaultPassword");

        try (final InputStream keyStoreStream = getKeyStoreStream(config)) {
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(keyStoreStream, defaultKeyStorePassword.toCharArray());
            return keyStore;
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException ex) {
            logger.error(ex.getMessage());
            throw new BlueButtonClientSetupException(ex.getMessage(), ex);
        }
    }

    @Provides
    public HttpClient provideHttpClient(Config config, KeyStore keyStore) {
        final String defaultKeyStorePassword = config.getString("bbclient.keyStore.defaultPassword");
        return buildMutualTlsClient(keyStore, defaultKeyStorePassword.toCharArray());
    }

    /**
     * Helper function get the keystore from either the location specified in the Configuration file, or from the JAR resources.
     * If the Config path is set, the helper will try to pull from the absolute file path.
     * Otherwise it looks for the {@link BlueButtonClientModule#KEYSTORE_RESOURCE_KEY} in the resources path.
     *
     * @return - {@link InputStream} to keystore
     */
    private InputStream getKeyStoreStream(Config config) {
        final InputStream keyStoreStream;

        if (!config.hasPath("bbclient.keyStore.location")) {
            keyStoreStream = DefaultBlueButtonClient.class.getResourceAsStream(KEYSTORE_RESOURCE_KEY);
            if (keyStoreStream == null) {
                logger.error("KeyStore location is empty, cannot find keyStore {} in resources", KEYSTORE_RESOURCE_KEY);
                throw new BlueButtonClientSetupException("Unable to get keystore from resources",
                        new MissingResourceException("", DefaultBlueButtonClient.class.getName(), KEYSTORE_RESOURCE_KEY));
            }
        } else {
            final String keyStorePath = config.getString("bbclient.keyStore.location");
            logger.debug("Opening keystream from location: {}", keyStorePath);
            try {
                keyStoreStream = new FileInputStream(keyStorePath);
            } catch (FileNotFoundException e) {
                logger.error("Could not find keystore at location: {}" + Paths.get(keyStorePath).toAbsolutePath().toString());
                throw new BlueButtonClientSetupException("Unable to find keystore", e);
            }
        }

        return keyStoreStream;
    }

    /**
     * Helper function to build a special {@link HttpClient} capable of authenticating with the Blue Button server using a client TLS certificate
     *
     * @param keyStore     {@link KeyStore} containing, at a minimum, the client tls certificate and private key
     * @param keyStorePass password for keystore (default: "changeit")
     * @return {@link HttpClient} compatible with HAPI FHIR TLS client
     */
    private HttpClient buildMutualTlsClient(KeyStore keyStore, char[] keyStorePass) {
        final SSLContext sslContext;

        try {
            sslContext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, keyStorePass)
                    .loadTrustMaterial(keyStore, null)
                    .build();

        } catch (KeyManagementException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException ex) {
            logger.error(ex.getMessage());
            throw new BlueButtonClientSetupException(ex.getMessage(), ex);
        }

        // Configure the socket timeout for the connection, incl. ssl tunneling
        final PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(200);
        connManager.setDefaultMaxPerRoute(100);

        SocketConfig sc = SocketConfig.custom()
                .setSoTimeout(2000)
                .build();

        connManager.setDefaultSocketConfig(sc);

        return HttpClients.custom()
                .setConnectionManager(connManager)
                .setConnectionManagerShared(true)
                .setSSLContext(sslContext)
                .build();
    }
}
