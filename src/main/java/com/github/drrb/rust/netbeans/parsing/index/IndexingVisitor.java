/**
 * Copyright (C) 2013 drrb
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.drrb.rust.netbeans.parsing.index;

import com.github.drrb.rust.netbeans.parsing.RustBaseVisitor;
import static com.github.drrb.rust.netbeans.parsing.RustLexUtils.*;
import com.github.drrb.rust.netbeans.parsing.RustParser;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 *
 */
public class IndexingVisitor extends RustBaseVisitor<RustSourceIndex> {

    private final RustSourceIndex index = new RustSourceIndex();

    @Override
    protected RustSourceIndex defaultResult() {
        return index;
    }

    @Override
    protected RustSourceIndex aggregateResult(RustSourceIndex aggregate, RustSourceIndex nextResult) {
        return aggregate;
    }

    @Override
    public RustSourceIndex visitItem_fn_decl(final RustParser.Item_fn_declContext functionContext) {
        visitChildren(functionContext);
        final RustFunction.Builder functionBuilder = RustFunction.builder()
                .setName(functionContext.accept(new FunctionNameFinder()))
                .setOffsetRange(offsetRangeFor(functionContext));
        functionContext.accept(new RustBaseVisitor<Void>() {
            @Override
            public Void visitArg(RustParser.ArgContext ctx) {
                return ctx.pat().accept(new RustBaseVisitor<Void>() {
                    @Override
                    public Void visitNon_global_path(RustParser.Non_global_pathContext ctx) {
                        RustParser.IdentContext argContext = ctx.ident(ctx.ident().size() - 1);
                        functionBuilder.addParameterName(new RustFunctionParameterName(argContext.getText(), offsetRangeFor(argContext)));
                        return null;
                    }
                });
            }

            @Override
            public Void visitFun_body(RustParser.Fun_bodyContext functionBodyContext) {
                final RustFunctionBody.Builder functionBodyBuilder = RustFunctionBody.builder().setText(functionBodyContext.getText()).setOffsetRange(offsetRangeFor(functionBodyContext));
                functionBodyContext.accept(new RustBaseVisitor<Void>() {
                    @Override
                    public Void visitIdent(RustParser.IdentContext ctx) {
                        functionBodyBuilder.addLocalVariableIdentifier(new RustLocalVariableIdentifier(ctx.getText(), offsetRangeFor(ctx)));
                        return null;
                    }
                });
                functionBuilder.setBody(functionBodyBuilder.build());
                return null;
            }
        });
        index.addFunction(functionBuilder.build());
        return index;
    }

    @Override
    public RustSourceIndex visitStruct_decl(RustParser.Struct_declContext structContext) {
        visitChildren(structContext);
        final RustStruct.Builder structBuilder = RustStruct.builder()
                .setName(structContext.ident().getText())
                .setOffsetRange(offsetRangeFor(structContext));
        TerminalNode openBrace = structContext.LBRACE();
        TerminalNode closeBrace = structContext.RBRACE();
        structBuilder.setBody(new RustStructBody(offsetRangeBetween(openBrace, closeBrace)));
        index.addStruct(structBuilder.build());
        return index;
    }

    @Override
    public RustSourceIndex visitEnum_decl(RustParser.Enum_declContext enumContext) {
        visitChildren(enumContext);
        final RustEnum.Builder enumBuilder = RustEnum.builder()
                .setName(enumContext.ident().getText())
                .setOffsetRange(offsetRangeFor(enumContext));
        TerminalNode openBrace = enumContext.LBRACE();
        TerminalNode closeBrace = enumContext.RBRACE();
        enumBuilder.setBody(new RustEnumBody(offsetRangeBetween(openBrace, closeBrace)));
        index.addEnum(enumBuilder.build());
        return index;
    }

    @Override
    public RustSourceIndex visitImpl(RustParser.ImplContext implContext) {
        visitChildren(implContext);
        //TODO: use a visitor. This will almost definitely have NPEs
        RustParser.Non_global_pathContext path = implContext.ty().path().non_global_path();
        final RustImpl.Builder implBuilder = RustImpl.builder()
                .setName(path.ident(0).getText())
                .setOffsetRange(offsetRangeFor(implContext));
        RustParser.Impl_bodyContext bodyContext = implContext.impl_body();
        implBuilder.setBody(new RustImplBody(offsetRangeFor(bodyContext)));
        index.addImpl(implBuilder.build());
        return index;
    }

    @Override
    public RustSourceIndex visitImpl_trait_for_type(RustParser.Impl_trait_for_typeContext traitImplContext) {
        visitChildren(traitImplContext);
        //TODO: use a visitor. This will almost definitely have NPEs
        RustParser.Non_global_pathContext traitNamePath = traitImplContext.trait().path().non_global_path();
        final RustTraitImpl.Builder traitImplBuilder = RustTraitImpl.builder()
                .setName(traitNamePath.getText()) //TODO: "name" doesn't really make sense here. For now, just doing it because the others have it
                .setOffsetRange(offsetRangeFor(traitImplContext));
        RustParser.Impl_bodyContext implBodyContext = traitImplContext.impl_body();
        traitImplBuilder.setBody(new RustTraitImplBody(offsetRangeFor(implBodyContext)));
        index.addTraitImpl(traitImplBuilder.build());
        return index;
    }

    @Override
    public RustSourceIndex visitTerminal(TerminalNode node) {
        if (node.getSymbol().getType() == RustParser.OUTER_DOC_COMMENT) {
            index.addDocComment(new RustDocComment(node.getText(), offsetRangeFor(node)));
        }
        return index;
    }
}

class FunctionNameFinder extends RustBaseVisitor<String> {

    @Override
    public String visitIdent(RustParser.IdentContext ctx) {
        return ctx.getText();
    }

    @Override
    protected String aggregateResult(String aggregate, String nextResult) {
        if (aggregate == null) {
            return nextResult;
        } else {
            return aggregate;
        }
    }

    @Override
    protected boolean shouldVisitNextChild(RuleNode node, String currentResult) {
        return currentResult == null;
    }
}