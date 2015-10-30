package org.yesworkflow.model;


import java.util.List;

import org.yesworkflow.annotations.Begin;
import org.yesworkflow.annotations.End;

public class Function extends Workflow {

    public final Port[] returnPorts;
    
    public Function(
            Long id,
            String name,
            Begin beginAnnotation, 
            End endAnnotation,
            List<Data> data,
            List<Port> inPorts,
            List<Port> outPorts,
            List<Port> returnPorts,
            List<Program> programs,
            List<Channel> channels,
            List<Function> functions
    ) {
        super(id, name, beginAnnotation, endAnnotation, data,
                inPorts, outPorts, programs,
                channels, functions);
        
        this.returnPorts = returnPorts.toArray(new Port[returnPorts.size()]);
    }
}
