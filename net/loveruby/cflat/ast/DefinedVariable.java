package net.loveruby.cflat.ast;
import net.loveruby.cflat.type.*;
import net.loveruby.cflat.asm.*;

public class DefinedVariable extends Variable {
    protected ExprNode initializer;
    protected AsmEntity address;

    public DefinedVariable(boolean priv, TypeNode type,
                           String name, ExprNode init) {
        super(priv, type, name);
        initializer = init;
        toplevel = false;
        sequence = -1;
    }

    public boolean isDefined() {
        return true;
    }

    public boolean hasInitializer() {
        return (initializer != null);
    }

    public boolean isInitialized() {
        return hasInitializer();
    }

    public ExprNode initializer() {
        return initializer;
    }

    public void setInitializer(ExprNode expr) {
        this.initializer = expr;
    }

    public void defineIn(ToplevelScope toplevel) {
        if (isPrivate()) {
            toplevel.allocatePrivateVariable(this);
        } else {
            toplevel.allocateVariable(this);
        }
    }

    public void accept(DefinitionVisitor visitor) {
        visitor.visit(this);
    }
}
