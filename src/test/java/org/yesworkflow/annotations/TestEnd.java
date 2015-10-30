package org.yesworkflow.annotations;

import org.yesworkflow.YesWorkflowTestCase;
import org.yesworkflow.annotations.End;

public class TestEnd extends YesWorkflowTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testEndComment_NameOnly() throws Exception {
        End end = new End(1L, 1L, 1L, "@end main");
        assertEquals("main", end.name);
    }

    public void testEndComment_WithDescription() throws Exception {
        End end = new End(1L, 1L, 1L, "@end main extra stuff");
        assertEquals("main", end.name);
        assertEquals("extra stuff", end.description);
    }
}
