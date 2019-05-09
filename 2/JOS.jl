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

@macroexpand @defclass (C1, [], a)
@defclass (C2, [], b, c)
@defclass (C3, [C1, C2], d)

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

#override get property for
#   get_slot
#   set_slot!

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

struct Alo
    slots :: Dict
    function Alo(classSymbol, superClasses, slots)
        slots = Dict()
        for s in eachindex(slots)
            slots[s] = slot
        end
        new(slots)
    end
end

function make_class(classSymbol, superClasses, slots)
    #println(classSymbol)
    #println(superClasses)
    #println(slots)
    a = Alo(classSymbol, superClasses, slots)
    #println(a)
end
