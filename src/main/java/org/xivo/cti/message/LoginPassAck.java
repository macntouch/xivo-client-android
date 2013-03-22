package org.xivo.cti.message;

import java.util.ArrayList;
import java.util.List;

public class LoginPassAck extends CtiMessage {

	public List<Integer> capalist = new ArrayList<Integer>();
	public double timenow;
	public long replyId;

}
