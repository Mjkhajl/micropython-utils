import gc
from network import WLAN
from socket import socket, AF_INET, SOCK_STREAM, getaddrinfo
from ure import compile
from sys import print_exception

def start( port ):
    server = Server( port )
    server.start()

class Server:

    def __init__( self, port ):
        self.host = WLAN().ifconfig()[0]
        self.port = port
        self.version = "0.02_32_led";

    def start( self ):
        print( "(v.", self.version, ") starting...", self.host, ":", self.port )
        sock = socket( AF_INET, SOCK_STREAM )
        sock.bind( getaddrinfo( self.host, self.port )[0][4] )
        sock.listen( 5 )
        try:
            while True:
                c_sock, c_addr = sock.accept()
                try:
                    res = req_processor.process( self.parseRequest( c_sock, c_addr ) )
                    res["headers"].append( "Content-Length: " + str( len( res["body"] ) ) )
                    response = bytes( "HTTP/1.0 {0} {1}\n{2}\n\n{3}\n\n".format( res["code"], res["message"], '\n'.join( res["headers"] ), res["body"] ), "utf-8" )
                    print( response )
                    c_sock.send( response )
                    if res["ex"]: raise res["ex"]
                except Exception as e:
                    print_exception( e )
                finally:
                    c_sock.close();
                    gc.collect();
                    print( "------", gc.mem_free() )
        finally:
            if sock: sock.close();
            print( "good bye!" )

    def parseRequest( self, c_sock, c_addr ):
        line = c_sock.readline()
        req = { "headers": {}, "c_sock": c_sock, "c_addr": c_addr, "method": None, "params":{} }
        while line:
            line = line.decode().strip( '\n\r' )
            print( line )
            if len( line ) == 0:
                break;
            if req["method"]:
                header = line.split( ':' )
                req["headers"][header[0]] = header[1]
            else:
                req["method"], req["path"], req["version"] = line.split( ' ' )
                if req["path"].find('?') != -1 :
                    req["path"], params = req["path"].split('?')
                    for param in params.split("&"):
                        name,val = param.split('=')
                        req["params"][name]=val
            line = c_sock.readline()
        return req

class GenericRequestProcessor:

    def __init__( self ):
        self.handlers = []

    def register( self, regexp ):
        def gethandler( f ):
            self.handlers.insert( 0, ( compile( regexp ), f ) )
            return f
        return gethandler;

    def process( self, req ):
        if req:
            print( req )
            res = { "headers" : [], "code": 200, "message":"OK", "ex": None }
            try:
                for pattern, handler in self.handlers:
                    if pattern.match( req["path"] ):
                        handler( req, res );
                        break;
            except Exception as e:
                res["code"], res["message"], res["body"], res["ex"] = 500, "Server Error", "Request: " + str( req ) + "\nError: " + str( e ), e
            return res;

req_processor = GenericRequestProcessor()

@req_processor.register( "." )
def default_handler( request, response ):
    response["code"], response["message"] = 404, "Not Found"
    response["body"] = "Micropython server\nResource: {0}\nMethod: {1}\nNot Found!!!".format( request["path"], request["method"] )

@req_processor.register( "/algo" )
def algo_handler( request, response ):
    response["headers"].append( "Content-Type: text/html" );
    response["body"] = "<html><body><h3>Resource: {0}\nMethod: {1}\nWas Found!!!<h3></html>".format( request["path"], request["method"] )

@req_processor.register( "/led" )
def led_handler( request, response ):
    from machine import Pin
    pin = Pin( 5, Pin.OUT )
    pin.value( request["params"]["value"] == "on" )
    response["body"] = "Led is turned " + request["params"]["value"]
