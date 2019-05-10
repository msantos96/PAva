mutable struct Class
    name
    superclasses
    slots
end

make_class(name, superclasses, slots) = (
    #order of slots and when names are reused???
    s = [];
    for sc in superclasses
        s = [s..., eval(:($sc.slots))...]
    end;
    slots = [s..., slots...];
    Class(name, superclasses, slots)
)

#C1 = make_class(:C1, [], [:a])
#C2 = make_class(:C2, [], [:b, :c])
#C3 = make_class(:C3, [C1, C2], [:d])

macro defclass(expr)
    #should check if symbol already in use???????
    name = expr.args[1]
    superclasses = expr.args[2].args
    slots = expr.args[3:end]
    :($(esc(name)) = make_class($(Base.Meta.quot(name)), $superclasses, $slots))
end

@defclass (C1, [], a) #space????
@defclass (C2, [], b, c)
@defclass (C3, [C1, C2], d)

#when typeof is used in a instance????
mutable struct Instance
    class :: Class
    slot_val
    function Instance(class, slot_val...)
        #dump(slot_val)
        dict = Dict()
        for sv in slot_val
            if any(x -> x == sv[1], class.slots) #error vs ignore
                dict[sv[1]] = sv[2]
            end
        end
        new(class, dict)
    end
end

make_instance(class, slot_val...) = Instance(class, slot_val...)

c3i1 = make_instance(C3, :a=>1, :b=>2, :c=>3, :d=>4)
c3i2 = make_instance(C3, :b=>2)

#change output when > c3i1 to show class ????

#can´t use slot val as a slot ????
function get_slot(x::Instance, field::Symbol)
    try
        getfield(x, :slot_val)[field]
    catch error
        if isa(error, KeyError)
            #should not show stack trace??
            throw(string("Slot ", string(field),
                any(x -> x == field, getfield(x, :class).slots) ? " is unbound" : " is missing"))
        end
    end
end

set_slot!(x::Instance, field::Symbol, value) = getfield(x, :slot_val)[field] = value

Base.getproperty(x::Instance, field::Symbol) = get_slot(x, field)
Base.setproperty!(x::Instance, field::Symbol, value) = set_slot!(x, field, value)

mutable struct Generic
    name
    parameters #verify duplicate
    methods
end

macro defgeneric(expr)
    #should check if symbol already in use???????
    #if receive type defmethod types should be derived of that type???
    name = expr.args[1]
    parameters = expr.args[2:end]
    :($(esc(name)) = Generic($(Base.Meta.quot(name)), $parameters, $[]))
end

mutable struct Method
    parameters #verify duplicate
    body
    lambda
end

(f::Method)(args...) = f.lambda(args...)

macro defmethod(expr)
    name = expr.args[1].args[1]
    parameters = expr.args[1].args[2:end]
    body = expr.args[2].args[2]
    #gen = :($parameters -> $body); dump(gen); gen.args[1].args = parameters
    :(
        try

            $(name).methods = [$(name).methods..., Method($parameters, [], () -> $body)] #missing body
            dump($(name).methods[end].lambda)
            $(name).methods[end].lambda.args[1].args = $(name).parameters
        catch error
            if isa(error, UndefVarError)
                throw(string("Undefined function ", $(Base.Meta.quot(name))))
            end
        end
    )
end

@defgeneric foo(c)

#error msg if called first????
@defmethod foo(c::C1) = 1

#replace if called twice????
@defmethod foo(c::C2) = c.b

function getEffectiveMethod(methods, args...)
    #check type to verify applicable
    methods[2](args...)
end

(f::Generic)(args...) = getEffectiveMethod(f.methods, args...)

foo(make_instance(C1))
#1
foo(make_instance(C2, :b=>42))
#42

###########################################################
square(x) = x*x

square(10)

struct IntrospectableFunction
    name
    parameters
    body
    native_function
end

square = IntrospectableFunction(:square, :(x,), :(x*x), x -> x*x)

square.native_function(3)

(f::IntrospectableFunction)(args...) = f.native_function(args...)

@introspectable square(x) = x*x
=>
square = IntrospectableFunction(:square, :(x,), :(x*x), x -> x*x)

macro introspectable(expr)
    #dump(expr)
    name = expr.args[1].args[1]
    parameters = tuple(expr.args[1].args[2:end]...)
    body = expr.args[2].args[2]
    :($name = IntrospectableFunction(:square, :(x,), :(x*x), x -> x*x))
end

expr = :(1 + 2)
eval(expr)

macro reset(var)
    :($(esc(var)) = 0)
end

let x = 10
    println(x)
    @reset(x)
    println(x)
end

foo = 0
@macroexpand @reset(foo)
