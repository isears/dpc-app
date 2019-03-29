package main

import (
	"github.com/gin-gonic/gin"
)

// IEnqueueReq is the structure by
// which we serialize our Job requests.
type IEnqueueReq []byte

// IEnqueueRes is the structure by
// which we process our Job requests
// and marshall the Job ID in the response.
type IEnqueueRes struct{ ID int64 }

// TODO: CONSIDER ERROR CODE MAPPING;
// decide if returning an ID of -1
// means it was invalid or not processed...

func main() {
	q := Init()
	router := gin.Default()
	router.PUT("/enqueue/:job", func(c *gin.Context) {
		// param serializes to string so this is inefficient
		bytesin := []byte(c.Param("job"))
		req := IEnqueueReq(bytesin)
		id, err := q.Append(req)
		if err != nil {
			c.Error(err)
			c.JSON(500, err.Error())
		} else {
			c.JSON(200, IEnqueueRes{ID: id})
		}
	})

	router.Run(":8080")
}
