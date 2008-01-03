package net.loveruby.cflat.type;

public abstract class Type {
    static final public long sizeUnknown = -1;

    public long alignment() {
        return size();
    }

    public abstract long size();
    public abstract String textize();

    public boolean isVoid() { return false; }
    public boolean isInt() { return false; }
    public boolean isInteger() { return false; }
    public boolean isSigned()
            { throw new Error("#isSigned for non-integer type"); }
    public boolean isPointer() { return false; }
    public boolean isArray() { return false; }
    public boolean isAllocatedArray() { return false; }
    public boolean isStruct() { return false; }
    public boolean isUnion() { return false; }
    public boolean isComplexType() { return false; }
    public boolean isUserType() { return false; }
    public boolean isFunction() { return false; }

    // Ability methods (unary)
    public boolean isDereferable() { return false; }
    public boolean isCallable() { return false; }

    // Ability methods (binary)
    public boolean isCompatible(Type other) { return false; }
    public boolean isCastableTo(Type target) { return equals(target); }

    public Type baseType() { throw new Error("#baseType called for undereferable type"); }

    // Cast methods
    public PointerType getPointerType() { return (PointerType)this; }
    public FunctionType getFunctionType() { return (FunctionType)this; }
    public StructType getStructType() { return (StructType)this; }
    public UnionType getUnionType() { return (UnionType)this; }
    public ComplexType getComplexType() { return (ComplexType)this; }
    public ArrayType getArrayType() { return (ArrayType)this; }
}
