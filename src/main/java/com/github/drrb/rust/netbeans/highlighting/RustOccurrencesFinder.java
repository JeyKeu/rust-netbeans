/**
 * Copyright (C) 2013 drrb
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.drrb.rust.netbeans.highlighting;

import com.github.drrb.rust.netbeans.parsing.NetbeansRustParser.NetbeansRustParserResult;
import com.github.drrb.rust.netbeans.parsing.index.RustFunction;
import com.github.drrb.rust.netbeans.parsing.index.RustFunctionBody;
import com.github.drrb.rust.netbeans.parsing.index.RustFunctionParameterName;
import com.github.drrb.rust.netbeans.parsing.index.RustLocalVariableIdentifier;
import com.github.drrb.rust.netbeans.parsing.index.RustSourceIndex;
import com.github.drrb.rust.netbeans.util.Option;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.modules.csl.api.ColoringAttributes;
import static org.netbeans.modules.csl.api.ColoringAttributes.*;
import org.netbeans.modules.csl.api.OccurrencesFinder;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;

/**
 *
 */
public class RustOccurrencesFinder extends OccurrencesFinder<NetbeansRustParserResult> {

    private final Map<OffsetRange, ColoringAttributes> occurrences = new HashMap<>();
    private int caretPosition;

    @Override
    public void setCaretPosition(int caretPosition) {
        this.caretPosition = caretPosition;
    }

    @Override
    public void run(NetbeansRustParserResult parseResult, SchedulerEvent event) {
        occurrences.clear();

        RustSourceIndex index = parseResult.getIndex();

        Option<RustFunction> maybeFunctionAtCaret = index.getFunctionAt(caretPosition);
        if (maybeFunctionAtCaret.is()) {
            RustFunction functionAtCaret = maybeFunctionAtCaret.value();
            Option<RustFunctionParameterName> maybeParamName = functionAtCaret.getParameterNameAt(caretPosition);
            if (maybeParamName.is()) {
                RustFunctionParameterName paramName = maybeParamName.value();
                OffsetRange paramNameTokenRange = paramName.getOffsetRange();
                addOccurrence(paramNameTokenRange, PARAMETER);
                List<RustLocalVariableIdentifier> matchingLocalVariables = functionAtCaret.getBody().getLocalVariableIdentifiersNamed(paramName.getText());
                for (RustLocalVariableIdentifier identifier : matchingLocalVariables) {
                    addOccurrence(identifier.getOffsetRange(), LOCAL_VARIABLE);
                }
            } else {
                RustFunctionBody functionBody = functionAtCaret.getBody();
                Option<RustLocalVariableIdentifier> maybeLocalVariableAtCaret = functionBody.getLocalVariableIdentifierAt(caretPosition);
                if (maybeLocalVariableAtCaret.is()) {
                    RustLocalVariableIdentifier localVariableAtCaret = maybeLocalVariableAtCaret.value();
                    List<RustLocalVariableIdentifier> matchingLocalVariables = functionBody.getLocalVariableIdentifiersNamed(localVariableAtCaret.getText());
                    for (RustLocalVariableIdentifier identifier : matchingLocalVariables) {
                        addOccurrence(identifier.getOffsetRange(), LOCAL_VARIABLE);
                    }
                    Option<RustFunctionParameterName> maybeMachingParamName = functionAtCaret.getParameterNameMatching(localVariableAtCaret.getText());
                    if (maybeMachingParamName.is()) {
                        RustFunctionParameterName matchingParamName = maybeMachingParamName.value();
                        addOccurrence(matchingParamName.getOffsetRange(), PARAMETER);
                    }
                }
            }
        }
    }

    @Override
    public Map<OffsetRange, ColoringAttributes> getOccurrences() {
        return new HashMap<>(occurrences);
    }

    @Override
    public int getPriority() {
        return 20; //Arbitrarily copied from CSS module
    }

    @Override
    public Class<? extends Scheduler> getSchedulerClass() {
        return null; //Copied from CSS module
    }

    @Override
    public void cancel() {
        //TODO: do something here
    }

    private void addOccurrence(OffsetRange range, ColoringAttributes type) {
        this.occurrences.put(range, type);
    }
}
