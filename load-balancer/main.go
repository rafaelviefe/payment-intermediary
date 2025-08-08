package main

import (
	"net/http"
	"net/http/httputil"
	"net/url"
	"sync/atomic"
	"time"

	"github.com/julienschmidt/httprouter"
)

var backendServers = []string{
	"http://api1:8080",
	"http://api2:8080",
}

var backendProxies []*httputil.ReverseProxy
var counter uint64

func getNextBackendProxy() *httputil.ReverseProxy {
	nextIndex := atomic.AddUint64(&counter, 1)
	return backendProxies[nextIndex%uint64(len(backendServers))]
}

func handleProxy(w http.ResponseWriter, r *http.Request, _ httprouter.Params) {
	getNextBackendProxy().ServeHTTP(w, r)
}

func main() {
	backendProxies = make([]*httputil.ReverseProxy, len(backendServers))

	transport := &http.Transport{
		Proxy:                 http.ProxyFromEnvironment,
		MaxIdleConns:          256,
		MaxIdleConnsPerHost:   128,
		IdleConnTimeout:       90 * time.Second,
		TLSHandshakeTimeout:   5 * time.Second,
		ExpectContinueTimeout: 1 * time.Second,
	}

	for i, targetStr := range backendServers {

		targetURL, err := url.Parse(targetStr)
        if err != nil {
            panic(err)
        }

		proxy := httputil.NewSingleHostReverseProxy(targetURL)
		proxy.Transport = transport

		backendProxies[i] = proxy
	}

	router := httprouter.New()
	router.POST("/payments", handleProxy)
	router.GET("/payments-summary", handleProxy)

	server := &http.Server{
		Addr:         ":9999",
		Handler:      router,
		ReadTimeout:  5 * time.Second,
		WriteTimeout: 5 * time.Second,
		IdleTimeout:  120 * time.Second,
	}

	if err := server.ListenAndServe(); err != nil {
		panic(err)
	}
}
