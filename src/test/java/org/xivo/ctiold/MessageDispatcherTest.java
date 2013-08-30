package org.xivo.ctiold;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xivo.ctiold.MessageDispatcher;
import org.xivo.ctiold.message.UserConfigUpdate;
import org.xivo.ctiold.message.UserUpdateListener;


public class MessageDispatcherTest {
    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    private MessageDispatcher messageDispatcher;
    final UserUpdateListener userConfigUpdateListener = context.mock(UserUpdateListener.class);

    @Before
    public void setUp() throws Exception {
        messageDispatcher = new MessageDispatcher();
    }

    @Test
    public void oneSubscriberReceivesCtiEvent() {
        final UserConfigUpdate userConfigUpdate = new UserConfigUpdate();
        
        messageDispatcher.addListener(UserConfigUpdate.class, userConfigUpdateListener);
        
        context.checking(new Expectations() {{
            oneOf (userConfigUpdateListener).onUserConfigUpdate(userConfigUpdate);
        }});

        messageDispatcher.dispatch(userConfigUpdate);
    }

    @Test
    public void sameSubscriberReceivesOnlyOnceCtiEvent() {
        final UserConfigUpdate userConfigUpdate = new UserConfigUpdate();
        
        messageDispatcher.addListener(UserConfigUpdate.class, userConfigUpdateListener);
        messageDispatcher.addListener(UserConfigUpdate.class, userConfigUpdateListener);
        
        context.checking(new Expectations() {{
            oneOf (userConfigUpdateListener).onUserConfigUpdate(userConfigUpdate);
        }});

        messageDispatcher.dispatch(userConfigUpdate);
    }

}
