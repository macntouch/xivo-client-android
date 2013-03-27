package org.xivo.cti.message;

import org.xivo.cti.model.Capacities;

public class LoginCapasAck extends CtiMessage {

	public String presence;
	public String userId;
	public String applicationName;
	public Capacities capacities;

}
