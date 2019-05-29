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
