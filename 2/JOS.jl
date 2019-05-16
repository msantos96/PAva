

struct Class
    name :: Symbol
    superclasses :: Vector{Class}
    slots :: Set{Symbol}
end

function make_class(name, superclasses, slots)
    for sc in superclasses
        slots = [slots..., sc.slots...]
    end
    Class(name, superclasses, Set(slots))
end

macro defclass(name, superclasses, slots...)
    :($(esc(name)) = make_class($(Meta.quot(name)), $superclasses, $slots))
end

struct Instance
    class :: Class
    slot_val :: Dict{Symbol, Any}
    function Instance(class, slot_val...)
        dict = Dict()
        for (s,v) in slot_val
            if any(x -> x == s, class.slots)
                dict[s] = v
            else
                error("Slot ", s, " is missing")
            end
        end
        new(class, dict)
    end
end

make_instance(class, slot_val...) = Instance(class, slot_val...)

function get_slot(x::Instance, field::Symbol)
    slot_val = getfield(x, :slot_val)
    if all(sv -> sv[1] != field, slot_val)
        error("Slot ", field, any(slot -> slot == field, getfield(x, :class).slots) ?  " is unbound" : " is missing")
    end
    slot_val[field]
end

function set_slot!(x::Instance, field::Symbol, value)
    if all(slot -> slot != field, getfield(x, :class).slots)
        error("Slot ", field, " is missing")
    end
    getfield(x, :slot_val)[field] = value
end

Base.getproperty(x::Instance, field::Symbol) = get_slot(x, field)
Base.setproperty!(x::Instance, field::Symbol, value) = set_slot!(x, field, value)

mutable struct Method
    types :: Vector{Symbol}
    lambda :: Function
end

mutable struct Generic
    name :: Symbol
    parameters :: Vector{Symbol}
    methods :: Vector{Method}
end

macro defgeneric(expr)
    name = expr.args[1]
    parameters = expr.args[2:end]
    if length(parameters) != length(Set(parameters))
        error("Duplicate variable name")
    end
    :($(esc(name)) = Generic($(Base.Meta.quot(name)), $parameters, $[]))
end

function defmethod(gen, vartypes, lambda)
    if length(vartypes) != length(gen.parameters)
        error("Required ", length(gen.parameters),
            " parameter(s) but ", length(vartypes), " given")
    end

    for m in gen.methods
        if m.types == vartypes
            m.lambda = lambda
            return
        end
    end

    gen.methods = [gen.methods..., Method(vartypes, lambda)]
end

macro defmethod(expr)
    name = expr.args[1].args[1]
    parameters = expr.args[1].args[2:end]
    body = expr.args[2].args[2]

    varnames = [p.args[1] for p in parameters]
    vartypes = [p.args[2] for p in parameters]

    if length(varnames) != length(Set(varnames))
        error("Duplicate variable name")
    end

    :(defmethod($(name), $vartypes, ($(varnames...),)->$body))
end

function recursive(idx, args_classes, ignore)
    ret = ignore ? [] : [[c.name for c in args_classes]]
    for class in args_classes[idx].superclasses
        ret = [ret..., recursive(
                    idx
                    , [args_classes[1:idx-1]..., class, args_classes[idx+1:end]...]
                    , false)...]
    end
    return ret
end

function expand(args_classes)
    expanded = [[c.name for c in args_classes]]
    for idx in range(1,length=length(args_classes))
        expanded = [expanded..., recursive(idx, args_classes, true)...]
    end
    return expanded
end

function findApplicable(methods, args_classes)
    for method in methods
        if method.types == args_classes
            return method
        end
    end
    return nothing
end

function getEffectiveMethod(methods, args...)
    args_classes = [getfield(a, :class) for a in args]

    for ac in expand(args_classes)
        method = findApplicable(methods, ac)
        if method != nothing
            return method
        end
    end

    error("No applicable method")
end

(f::Method)(args...) = f.lambda(args...)
(f::Generic)(args...) = getEffectiveMethod(f.methods, args...)(args...)
