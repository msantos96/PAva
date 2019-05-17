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
    before_methods :: Dict{Array,Method}  #{[type1,type2,...]=>Method}
    methods :: Dict{Array,Method}  #{[type1,type2,...]=>Method}
    after_methods :: Dict{Array,Method}  #{[type1,type2,...]=>Method}

    function Generic(class, parameters)
        new(class, parameters, Dict(), Dict(), Dict())
    end
end

macro defgeneric(expr)
    name = expr.args[1]
    parameters = expr.args[2:end]
    if length(parameters) != length(Set(parameters))
        error("Duplicate variable name")
    end
    :($(esc(name)) = Generic($(Base.Meta.quot(name)), $parameters))
end

function defmethod(gen, vartypes, lambda, qualifier)
    if length(vartypes) != length(gen.parameters)
        error("Required ", length(gen.parameters), " parameter(s) but ", length(vartypes), " given")
    end

    if qualifier == :before
        gen.before_methods[vartypes] = Method(vartypes, lambda)
    elseif qualifier == :after
        gen.after_methods[vartypes] = Method(vartypes, lambda)
    else
        gen.methods[vartypes] = Method(vartypes, lambda)
    end
end

macro defmethod(expr)
    :(@defmethod(nothing, $expr))
end

macro defmethod(qualifier, expr)
    name = expr.args[1].args[1]
    parameters = expr.args[1].args[2:end]
    body = expr.args[2].args[2]

    varnames = [p.args[1] for p in parameters]
    vartypes = [p.args[2] for p in parameters]

    if length(varnames) != length(Set(varnames))
        error("Duplicate variable name")
    end

    :(defmethod($(name), $vartypes, ($(varnames...),)->$body, $qualifier))
end

function expand(idx, argstypes)
    expanded = [[c.name for c in argstypes]]
    for type in argstypes[idx].superclasses
        expanded = [expanded..., expand(
                    idx
                    , [argstypes[1:idx-1]..., type, argstypes[idx+1:end]...])...]
    end
    return expanded
end

function getPermutations(argstypes)
    expanded = [[c.name for c in argstypes]]
    for idx in range(1, length=length(argstypes))
        for type in argstypes[idx].superclasses
            expanded = [expanded..., expand(
                            idx
                            , [argstypes[1:idx-1]..., type, argstypes[idx+1:end]...])...]
        end
    end
    return expanded
end

function getEffectiveMethods(methods, expanded)
    return [methods[types] for types in expanded if haskey(methods, types)]
end

function applyEffectiveMethods(f, args...)
    expanded = getPermutations([getfield(a, :class) for a in args])

    before = getEffectiveMethods(f.before_methods, expanded)
    method = getEffectiveMethods(f.methods, expanded)
    after = getEffectiveMethods(f.after_methods, expanded)

    if length(method) == 0
        if length(before) != 0 || length(after) != 0
            error("No primary method")
        else
            error("No applicable method")
        end
    end

    for b in before b(args...) end
    res = method[1](args...)
    for a in after a(args...) end

    return res
end

(f::Method)(args...) = f.lambda(args...)
(f::Generic)(args...) = applyEffectiveMethods(f, args...)
