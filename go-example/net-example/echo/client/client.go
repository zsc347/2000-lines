package main

import (
	"bufio"
	"fmt"
	"io"
	"log"
	"net"
)

func main() {
	conn, err := net.Dial("tcp", "127.0.0.1:8000")

	if err != nil {
		log.Fatalf("connect failed: %s", err.Error())
	}

	messageChan := make(chan string)
	cmdChan := make(chan string)

	go func() {
		writer := bufio.NewWriter(conn)
		for {
			select {
			case msg := <-messageChan:
				writer.WriteString(msg + string('\n'))
			case cmd := <-messageChan:
				if cmd == "exit" {
					goto exit
				}
				fmt.Printf("unknown cmd: %s \n", cmd)
			}
		}
	exit:
		io.WriteString(conn, "client close\n")
		conn.Close()
	}()

	go func() {
		reader := bufio.NewReader(conn)
		line, err := reader.ReadBytes('\n')
		if err != nil {
			if err == io.EOF {
				goto exit
			}
			fmt.Println("error occur: ", err.Error())
		}
		fmt.Printf("client received: %s\n", line)
	exit:
		fmt.Printf("exiting")
	}()

	var cmd string
	for {
		fmt.Scanf("%s", &cmd)
		if cmd == "exit" {
			cmdChan <- cmd
		} else {
			messageChan <- cmd
		}
	}
}
