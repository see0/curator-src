package edu.illinois.cs.cogcomp.curator;

import org.apache.thrift.transport.TTransportException;

import edu.illinois.cs.cogcomp.thrift.base.BaseService;

/**
* Mock Pool that allows us to have local servers.
*  
* @author James Clarke
* 
*/

public class MockPool implements Pool {

	private BaseService.Iface service;
	public MockPool(BaseService.Iface service) {
		this.service = service;
	}
	
	public Object getClient() throws TTransportException {
		return service;
	}

	public void releaseClient(Object client) {
		//do nothing
		return;
	}

	public String getStatusReport() {
		return "1:m";
	}

}
