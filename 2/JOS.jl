#use types?????

mutable struct Class #should be mutable??? changing the name will not attach the new symbol
    name
    superclasses
    slots
end

make_class(name, superclasses, slots) = (
    #order of slots and when names are reused???
    #use instances instead???
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
    name = expr.args[1]
    superclasses = expr.args[2].args
    slots = expr.args[3:end]
    :($(esc(name)) = make_class($(Base.Meta.quot(name)), $superclasses, $slots))
end

@defclass (C1, [], a) #space????
@defclass (C2, [], b, c)
@defclass (C3, [C1, C2], d)

#when typeof is used in a instance???? should return class????
mutable struct Instance
    class
    slot_val
    function Instance(class, slot_val...)
        dict = Dict()
        for sv in slot_val
            if any(x -> x == sv[1], class.slots) #error vs ignore (user tries to attach value to unexisting slot)
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

#should redefine instance and class get slot so users cant change it???? if so should we remove the "introspectable" fields????
#can´t use slot val as a slot ???? is a problem???
function get_slot(x::Instance, field::Symbol)
    try
        getfield(x, :slot_val)[field]
    catch error
        if isa(error, KeyError)
            #should not show stack trace??
            throw(string("Slot ", string(field),
                any(x -> x == field, getfield(x, :class).slots) ? " is unbound" : " is missing"))
        else
            throw(error)
        end
    end
end

set_slot!(x::Instance, field::Symbol, value) = getfield(x, :slot_val)[field] = value

Base.getproperty(x::Instance, field::Symbol) = get_slot(x, field)
Base.setproperty!(x::Instance, field::Symbol, value) = set_slot!(x, field, value)

get_slot(c3i2, :b)
#2
set_slot!(c3i2, :b, 3)
#3
println([get_slot(c3i1, s) for s in [:a, :b, :c]])
#[1, 2, 3]
c3i1.a
#1
c3i1.e
#ERROR: Slot e is missing
c3i2.a
#ERROR: Slot a is unbound
c3i2.a = 5
#5
c3i2.a
#5

mutable struct Generic
    name
    parameters #verify duplicate
    methods
end

macro defgeneric(expr)
    #if receive type defmethod types should be derived of that type???
    name = expr.args[1]
    parameters = expr.args[2:end]
    :($(esc(name)) = Generic($(Base.Meta.quot(name)), $parameters, $[]))
end

mutable struct Method
    parameters #verify duplicate names
    body
    lambda
end

(f::Method)(args...) = f.lambda(args...)

macro defmethod(expr)
    name = expr.args[1].args[1]
    parameters = expr.args[1].args[2:end]
    body = expr.args[2].args[2]
    lambda = Meta.parse("() -> $body")
    lambda.args[1].args = [p.args[1] for p in parameters]

    :(
        try
            for m in $(name).methods
                if length($parameters) == length(m.parameters) &&
                        tuple([p.args[2] for p in m.parameters]...) == tuple([a.args[2] for a in $parameters]...) # error when types not specified
                    #m.body = $body
                    #m.lambda = $lambda
                    return
                end
            end
            $(name).methods = [$(name).methods..., Method($parameters, [], $lambda)] #missing body
            $(name).methods[end]
        catch error
            if isa(error, UndefVarError)
                throw(string("Undefined function ", $(Base.Meta.quot(name))))
            else
                throw(error)
            end
        end
    )
end

@defgeneric foo(c)

#error msg if called before foo
@defmethod foo(c::C1) = 1

@defmethod foo(c::C2) = c.b

@defmethod foo(c::C2, c1::C1) = c.b + 1

function getEffectiveMethod(methods, args...)
    #also verify when type does not exist - extends?????
    #flavors approach???
    for m in methods
        if length(args) == length(m.parameters) &&
                tuple([p.args[2] for p in m.parameters]...) == tuple([getfield(a, :class).name for a in args]...)
            return m
        end
    end
    throw(string("No applicable method"))
end

(f::Generic)(args...) = getEffectiveMethod(f.methods, args...)(args...)

foo(make_instance(C1))
#1
foo(make_instance(C2, :b=>42))
#42

@defgeneric bar(x, y)
@defmethod bar(x::C1, y::C2) = x.a + y.b
@defmethod bar(x::C1, y::C3) = x.a - y.b
@defmethod bar(x::C3, y::C3) = x.a * y.b

c1i1 = make_instance(C1, :a=>1)
c2i1 = make_instance(C2, :b=>3)
c3i1 = make_instance(C3, :a=>1, :b=>2)
c3i2 = make_instance(C3, :b=>3, :a=>5)

bar(c1i1, c2i1)
#4
bar(c2i1, c1i1)
#ERROR: No applicable method
bar(c1i1, c3i1)
#-1
bar(c3i1, c3i2)
#3
