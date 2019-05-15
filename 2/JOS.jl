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

macro defclass(expr) #method?????
    name = expr.args[1]
    superclasses = expr.args[2].args
    slots = expr.args[3:end]
    :($(esc(name)) = make_class($(Base.Meta.quot(name)), $superclasses, $slots))
end

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

#change output when > c3i1 to show class ????

#should redefine instance and class get slot so users cant change it???? if so should we remove the "introspectable" fields????
#can´t use slot val as a slot ???? is a problem???
function get_slot(x::Instance, field::Symbol)
    try
        getfield(x, :slot_val)[field]
    catch error
        if isa(error, KeyError)
            #should not show stack trace??
            throw(error(string("ERROR: Slot " , string(field),
                any(x -> x == field, getfield(x, :class).slots) ?  " is unbound" : " is missing"))
        else
            throw(error)
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

function getEffectiveMethod(methods, args...)
    #also verify when type does not exist - extends?????
    #flavors approach???
    for m in methods
        if length(args) == length(m.parameters) &&
                tuple([p.args[2] for p in m.parameters]...) == tuple([getfield(a, :class).name for a in args]...) #applicable!!!!!!
            return m
        end
    end
    throw(string("No applicable method"))
end

(f::Generic)(args...) = getEffectiveMethod(f.methods, args...)(args...)
