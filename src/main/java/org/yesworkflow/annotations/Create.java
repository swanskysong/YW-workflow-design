package org.yesworkflow.annotations;

import org.yesworkflow.YWKeywords;

public class Create extends Delimiter {

    public Create(Long id, Long sourceId, Long lineNumber, String comment) throws Exception {
        super(id, sourceId, lineNumber, comment, YWKeywords.Tag.CREATE);
    }
}

