package main

import (
	"errors"
	"fmt"
	"log"
	"time"

	"github.com/jackc/pgx"

	que "github.com/bgentry/que-go"
)

type testLogger struct{}

func (t testLogger) Log(level pgx.LogLevel, msg string, data map[string]interface{}) {
	log.Println(msg)
}

func crash(err error) {
	log.Fatal(fmt.Errorf("|TIME=%s|CRASH_REASON=%v|", time.Now().UTC(), err))
}

var errEnvNotSet = errors.New("Please set the environment variable: DATABASE_URL")

const (
	localDB = "User ID=tester;Password=test123;Host=localhost;Port=5432;Database=queue_api_testing; Pooling=true;Min Pool Size=0;Max Pool Size=100;Connection Lifetime=0;"
)

func getDatabaseConnectionConfig() pgx.ConnConfig {
	conncfg, err := pgx.ParseURI("queue_api_testing")
	if err != nil {
		crash(err)
	}
	conncfg.Logger = testLogger{}
	conncfg.LogLevel = pgx.LogLevelTrace

	return conncfg
}

// GetDataBaseClient returns a *que.Client
// which is used to route requests through
// to the queue backend and pgx connection pool.
func GetDataBaseClient() *que.Client {
	dbconncfg := getDatabaseConnectionConfig()
	dbconn, err := pgx.NewConnPool(pgx.ConnPoolConfig{
		ConnConfig: dbconncfg,

		// AfterConnect: que.PrepareStatements,
	})
	if err != nil {
		crash(err)
	}
	return que.NewClient(dbconn)
}

func printjob(j *que.Job) error {
	fmt.Printf("Hello %b!\n", j.Args)
	return nil
}

// GetWorkerPool is called to setup the worker threads
// for queueing work.
func GetWorkerPool(cl *que.Client) *que.WorkerPool {
	wm := que.WorkMap{"PrintJob": printjob}
	return que.NewWorkerPool(cl, wm, 3) // create a pool w/ 2 workers
}

// The QueueInterface is a lightweight internal
// representation of a queue abstraction.
type QueueInterface struct {
	cl *que.Client
	wp *que.WorkerPool
}

// Append is a static external call to
// add jobs to QueueInterface
// which routes work to a *que.Client.
func (q QueueInterface) Append(blob []byte) int64 {
	j := &que.Job{
		Type: "PrintJob",
		Args: blob,
	}
	runit := q.cl.Enqueue(j)
	if runit != nil {
		return -1
	}
	return j.ID
}

// Init is the setup function which generates
// an internal representation of a go-queue
func Init() QueueInterface {
	client := GetDataBaseClient()
	workerpool := GetWorkerPool(client)
	return QueueInterface{
		cl: client,
		wp: workerpool,
	}
}
