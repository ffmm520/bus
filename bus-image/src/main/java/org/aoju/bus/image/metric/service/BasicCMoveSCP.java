/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2020 aoju.org and other contributors.                      *
 *                                                                               *
 * Permission is hereby granted, free of charge, to any person obtaining a copy  *
 * of this software and associated documentation files (the "Software"), to deal *
 * in the Software without restriction, including without limitation the rights  *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     *
 * copies of the Software, and to permit persons to whom the Software is         *
 * furnished to do so, subject to the following conditions:                      *
 *                                                                               *
 * The above copyright notice and this permission notice shall be included in    *
 * all copies or substantial portions of the Software.                           *
 *                                                                               *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, *
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     *
 * THE SOFTWARE.                                                                 *
 ********************************************************************************/
package org.aoju.bus.image.metric.service;

import org.aoju.bus.image.Dimse;
import org.aoju.bus.image.Status;
import org.aoju.bus.image.galaxy.data.Attributes;
import org.aoju.bus.image.metric.Association;
import org.aoju.bus.image.metric.Commands;
import org.aoju.bus.image.metric.ImageException;
import org.aoju.bus.image.metric.internal.pdu.Presentation;

import java.io.IOException;

/**
 * @author Kimi Liu
 * @version 6.0.1
 * @since JDK 1.8+
 */
public class BasicCMoveSCP extends AbstractService {

    public BasicCMoveSCP(String... sopClasses) {
        super(sopClasses);
    }

    @Override
    public void onDimse(Association as, Presentation pc, Dimse dimse,
                        Attributes cmd, Attributes keys) throws IOException {
        if (dimse != Dimse.C_MOVE_RQ)
            throw new ImageException(Status.UnrecognizedOperation);

        Retrieve retrieve = calculateMatches(as, pc, cmd, keys);
        if (retrieve != null)
            as.getApplicationEntity().getDevice().execute(retrieve);
        else
            as.tryWriteDimseRSP(pc, Commands.mkCMoveRSP(cmd, Status.Success));
    }

    protected Retrieve calculateMatches(Association as, Presentation pc,
                                        Attributes rq, Attributes keys) {
        return null;
    }

}