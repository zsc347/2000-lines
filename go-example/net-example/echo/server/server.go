package main

import (
	"fmt"
	"io"
	"net"
	"os"
	"sync/atomic"
)

const buffSize = 20

func main() {
	addr, err := net.ResolveTCPAddr("tcp4", "localhost:8000")
	if err != nil {
		panic(err)
	}

	tcpListener, err := net.ListenTCP("tcp4", addr)

	var exiting int32

	go func() {
		var cmd string
		for {
			fmt.Print("input:")
			fmt.Scanf("%s", &cmd)
			switch cmd {
			case "exit":
				atomic.StoreInt32(&exiting, 1)
				goto exit
			default:
				fmt.Printf("unknown cmd %s\n", cmd)
			}
		}
	exit:
		// TODO shutdown gracefully
		os.Exit(1)
	}()

	for {
		tcpConn, err := tcpListener.AcceptTCP()
		if err != nil {
			fmt.Printf("error accept %s\n", err.Error())
			continue
		}
		go handleConn(tcpConn)
	}
}

func handleConn(conn *net.TCPConn) {
	fmt.Printf("detect conn %s\n", conn.RemoteAddr().String())
	buffer := make([]byte, buffSize)
	record := make([]byte, 0)
	for {
		n, err := conn.Read(buffer)
		fmt.Println(n)
		if err != nil {
			if err == io.EOF {
				goto exit
			}
			fmt.Printf("error occur %v\n", err)
			continue
		}

		for i := 0; i < n; i++ {
			if buffer[i] != '\n' {
				record = append(record, buffer[i])
			} else {
				handleRecord(conn, record)
				record = record[:0]
			}
		}
	}
exit:
	fmt.Printf("client %s: %s\n", conn.RemoteAddr().String(), "disconnected")
}

func handleRecord(conn *net.TCPConn, record []byte) {
	fmt.Printf("client %s: %s\n", conn.RemoteAddr().String(), record)
	conn.Write([]byte(record))
}
