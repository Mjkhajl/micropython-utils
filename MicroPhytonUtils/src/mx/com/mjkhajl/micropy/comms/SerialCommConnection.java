package mx.com.mjkhajl.micropy.comms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import javax.comm.CommPortIdentifier;
import javax.comm.NoSuchPortException;
import javax.comm.SerialPort;
import javax.comm.SerialPortEvent;
import javax.comm.SerialPortEventListener;

import mx.com.mjkhajl.micropy.utils.CodeUtils;
import mx.com.mjkhajl.micropy.utils.Log;
import mx.com.mjkhajl.micropy.utils.Log.LogLevel;

public class SerialCommConnection implements Connection, SerialPortEventListener {

	private SerialPort		port		= null;
	private boolean			connected	= false;
	private InputStream		inStream	= null;
	private OutputStream	outStream	= null;
	private final int		freq, dataBits, stopBits, parity, timeout;

	/**
	 * Creates a connection object with the specified connection details
	 * 
	 * @see javax.comm.SerialPort javax.comm.SerialPort
	 * 
	 *      for details on dataBits, stopBits and Parity values
	 * 
	 * @param freq
	 *            Bits per second transfer speed
	 * @param dataBits
	 *            DATA BITS for serial comm see
	 *            javax.comm.SerialPort.DATA_BITS_{X}
	 * @param stopBits
	 *            STOP BITS for serial comm see
	 *            javax.comm.SerialPort.STOP_BITS_{X}
	 * @param parity
	 *            PARITY for serial comm see javax.comm.SerialPort.PARITY_{XXXX}
	 * @param timeout
	 *            timeout to get connection...
	 */
	public SerialCommConnection( int freq, int dataBits, int stopBits, int parity, int timeout ) {

		this.freq = freq;
		this.dataBits = dataBits;
		this.stopBits = stopBits;
		this.parity = parity;
		this.timeout = timeout;
	}

	@Override
	public synchronized Connection connectTo( String portName ) throws IOException {

		if ( connected )
			throw new IllegalStateException( "Already connected!! ... close connection first" );

		// get machine ports...
		CommPortIdentifier portId;

		try {

			portId = CommPortIdentifier.getPortIdentifier( portName );
		} catch ( NoSuchPortException e ) {

			throw new IOException( portName + " is not found...", e );
		}

		if ( !tryPort( portId ) ) {

			throw new IOException( portName + " is not available..." );
		}
		return this;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public synchronized Connection connectToFirstAvailable() throws IOException {

		// get machine ports...
		Enumeration<CommPortIdentifier> portIds = CommPortIdentifier.getPortIdentifiers();

		while ( portIds.hasMoreElements() ) {

			if ( tryPort( portIds.nextElement() ) ) {

				return this;
			}
		}

		throw new IOException( "No serial port available!!" );
	}

	private synchronized boolean tryPort( CommPortIdentifier portId ) {

		if ( connected )
			throw new IllegalStateException( "Already connected!!" );

		try {
			// serial ports only
			if ( portId.getPortType() == CommPortIdentifier.PORT_SERIAL ) {

				// open the port registering the listener as this instance
				// name...
				port = (SerialPort) portId.open( String.valueOf( this ), (int) timeout );
				port.setSerialPortParams( freq, dataBits, stopBits, parity );
				port.enableReceiveTimeout( 0 );
				Log.log( "receiveframming: " + port.isReceiveFramingEnabled(), LogLevel.DEBUG );
				Log.log( "getReceiveTimeout: " + port.getReceiveTimeout(), LogLevel.DEBUG );
				Log.log( "getReceiveThreshold: " + port.getReceiveThreshold(), LogLevel.DEBUG );
				Log.log( "getInputBufferSize: " + port.getInputBufferSize(), LogLevel.DEBUG );
				Log.log( "getOutputBufferSize: " + port.getOutputBufferSize(), LogLevel.DEBUG );

				port.addEventListener( this );
				port.notifyOnDataAvailable( true );

				outStream = port.getOutputStream();
				inStream = port.getInputStream();

				connected = true;

				Log.log( "Connected to: " + port.getName(), LogLevel.INFO );

				return true;
			}
		} catch ( Exception e ) {

			// port failed, return false...
			Log.log( e, LogLevel.ERROR );
		}
		return false;
	}

	@Override
	public synchronized void write( byte[] data ) throws IOException {

		for ( byte b : data ) {
			outStream.write( b );
		}
		outStream.flush();
	}

	@Override
	public synchronized int read() throws IOException {

		int data = inStream.read();
		
		if ( data == -1 ) {

			try {

				wait( timeout );
				// read again...
			} catch ( InterruptedException e ) {
				e.printStackTrace();
			}
			data = inStream.read();
		}		
		return data;
	}

	@Override
	public synchronized void close() throws IOException {

		if ( connected ) {

			// close streams silently
			CodeUtils.close( outStream, inStream );

			// close port
			port.close();
			connected = false;
		}
	}

	@Override
	public boolean isConnected() {

		return connected;
	}

	@Override
	public synchronized void serialEvent( SerialPortEvent event ) {

		if ( event.getEventType() == SerialPortEvent.DATA_AVAILABLE ) {
			notify();
		}
	}
}
