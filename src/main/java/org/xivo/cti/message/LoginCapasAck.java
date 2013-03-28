package org.xivo.cti.message;

import java.util.List;

import org.xivo.cti.model.Capacities;
import org.xivo.cti.model.Xlet;

public class LoginCapasAck extends CtiMessage {

	public String presence;
	public String userId;
	public String applicationName;
	public Capacities capacities;
	public List<Xlet> xlets;

}
