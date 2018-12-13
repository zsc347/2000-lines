package main

import (
	"bufio"
	"fmt"
	"io"
	"log"
	"net"
	"sync/atomic"
)

func main() {
	conn, err := net.Dial("tcp", "127.0.0.1:8000")

	if err != nil {
		log.Fatalf("connect failed: %s", err.Error())
	}

	writer := bufio.NewWriter(conn)
	reader := bufio.NewReader(conn)

	messageChan := make(chan string)
	cmdChan := make(chan string)
	exitChan := make(chan bool, 1)

	var exitFlag int32

	// write loop
	go func() {
		var err error
		for {
			select {
			case msg := <-messageChan:
				_, err = writer.WriteString(msg + string('\n'))
				if err != nil {
					goto close
				}
				err = writer.Flush()
				if err != nil {
					goto close
				}
			case cmd := <-cmdChan:
				if cmd == "exit" {
					goto close
				}
				fmt.Printf("unknown cmd: %s \n", cmd)
			}
		}
	close:
		if err != nil {
			fmt.Printf("writer error : %s\n", err.Error())
		}
		if atomic.CompareAndSwapInt32(&exitFlag, 0, 1) {
			close(exitChan)
		}
	}()

	// read loop
	go func() {
		for {
			if atomic.LoadInt32(&exitFlag) == 0 {
				line, err := reader.ReadSlice('\n')
				if err != nil {
					if err == io.EOF {
						fmt.Printf("receive: %s\n", line)
						goto close
					}
					fmt.Println("error occur: ", err.Error())
				}
				fmt.Printf("receive: %s\n", line)
			}
		}
	close:
		if atomic.CompareAndSwapInt32(&exitFlag, 0, 1) {
			close(exitChan)
		}
	}()

	// cmd loop
	go func() {
		var cmd string
		for {
			fmt.Print("> ")
			fmt.Scanf("%s", &cmd)
			fmt.Println('\n')
			if cmd == "exit" {
				cmdChan <- cmd
			} else {
				messageChan <- cmd
			}
		}
	}()

	<-exitChan
	conn.Close()
	fmt.Println("client close")
}
