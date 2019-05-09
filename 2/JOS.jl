struct Class
    name
    superclasses
    slots
end

make_class(name, superclasses, slots) = Class(name, superclasses, slots)

#C1 = make_class(:C1, [], [:a])
#C2 = make_class(:C2, [], [:b, :c])
#C3 = make_class(:C3, [C1, C2], [:d])

macro defclass(expr)
    #should check if symbol already in use???????
    name = expr.args[1]
    superclasses = expr.args[2].args
    slots = expr.args[3:end]
    println(name)
    dump(name)
    :($(esc(name)) = make_class((:)$name, $superclasses, $slots))
end

@macroexpand @defclass (C1, [], a)
@defclass (C2, [], b, c)
@defclass (C3, [C1, C2], d)

#make_instance()







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
