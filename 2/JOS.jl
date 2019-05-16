#use types

struct Class
    name :: Symbol
    superclasses :: Array{Class,1}
    slots :: Array{Symbol}
end

#?????????????????????????
function make_class(name, superclasses, slots)
    s = []
    for sc in superclasses
        s = [s..., sc.slots...]
    end
    slots = [s..., slots...];
    Class(name, superclasses, slots)
end

#2?????
macro defclass(name, superclasses, slots)
    slots = [Symbol(slots)]
    :($(esc(name)) = make_class($(Base.Meta.quot(name)), $superclasses, $slots))
end

macro defclass(name, superclasses, slots...)
    slots = [slots...]
    :($(esc(name)) = make_class($(Base.Meta.quot(name)), $superclasses, $slots))
end

struct Instance
    class
    slot_val
    function Instance(class, slot_val...)
        dict = Dict()
        for sv in slot_val
            if any(x -> x == sv[1], class.slots)
                dict[sv[1]] = sv[2]
            else
                error("Slot ", sv[1], " is missing")
            end
        end
        new(class, dict)
    end
end

#2 slots with same name
make_instance(class, slot_val...) = Instance(class, slot_val...)

function get_slot(x::Instance, field::Symbol)
    slots = getfield(x, :slot_val)
    if all(slot -> slot[1] != field, slots)
        error(string("Slot " , string(field), any(slot -> slot == field, getfield(x, :class).slots) ?  " is unbound" : " is missing"))
    end
    slots[field]
end

set_slot!(x::Instance, field::Symbol, value) = getfield(x, :slot_val)[field] = value

Base.getproperty(x::Instance, field::Symbol) = get_slot(x, field)
Base.setproperty!(x::Instance, field::Symbol, value) = set_slot!(x, field, value)

mutable struct Generic
    name
    parameters
    methods
end

#verify duplicate parameter names (constructor) ???
macro defgeneric(expr)
    name = expr.args[1]
    parameters = expr.args[2:end]
    :($(esc(name)) = Generic($(Base.Meta.quot(name)), $parameters, $[]))
end

mutable struct Method
    types
    lambda
end

(f::Method)(args...) = f.lambda(args...)

#should be missing instead of undefvar???
#verify name of parameters with the genric???
function defmethod(gen, vartypes, lambda)
    if length(vartypes) != length(gen.parameters) throw(UndefVarError(gen.name)) end #throw(UndefVar) ??????
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

    :(defmethod($(name), $vartypes, ($(varnames...),)->$body)) #throws UndefVar change??????
end

function recursive(idx, args_classes, ignore)
    ret = ignore ? [] : [[c.name for c in args_classes]]
    for class in args_classes[idx].superclasses
        ret = [ret..., recursive(idx, [args_classes[1:idx-1]..., class, args_classes[idx+1:end]...], false)...]
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

    #[println(to) for to in expand(args_classes)]
    #println("##########")
    #[println(to) for to in methods]
    #println("##########")
    #[println(findApplicable(methods, ac)) for ac in expand(args_classes) if findApplicable(methods, ac) != nothing]

    for ac in expand(args_classes)
        method = findApplicable(methods, ac)
        if method != nothing
            return method
        end
    end

    error("No applicable method")
end

(f::Generic)(args...) = getEffectiveMethod(f.methods, args...)(args...)
