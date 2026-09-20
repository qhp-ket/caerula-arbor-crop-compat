var Opcodes = Java.type('org.objectweb.asm.Opcodes');

var BLOCK = 'net/minecraft/world/level/block/Block';
var BRIDGE = 'io/github/caerulacropcompat/CaerulaCropBlock';
var CONSTRUCTOR_DESC = '(Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V';
var INTEGER_PROPERTY = 'net/minecraft/world/level/block/state/properties/IntegerProperty';
var INTEGER_PROPERTY_CREATE = 'm_61631_';
var INTEGER_PROPERTY_CREATE_DESC =
    '(Ljava/lang/String;II)Lnet/minecraft/world/level/block/state/properties/IntegerProperty;';
var STATE_DEFINITION = 'net/minecraft/world/level/block/state/StateDefinition';
var GET_PROPERTY = 'm_61081_';
var GET_PROPERTY_DESC =
    '(Ljava/lang/String;)Lnet/minecraft/world/level/block/state/properties/Property;';
var OLD_AGE_NAME = 'blockstate';
var NEW_AGE_NAME = 'age';

function initializeCoreMod() {
    return {
        'planted_viviparous_lily_crop': transformCrop(
            'net.mcreator.caerulaarbor.block.PlantedViviparousLilyBlock'),
        'nethersea_potato_crop': transformCrop(
            'net.mcreator.caerulaarbor.block.NetherseaPotatoPlantBlock'),
        'nethersea_wheat_crop': transformCrop(
            'net.mcreator.caerulaarbor.block.NetherseaWheatBlock'),
        'tentacle_plant_crop': transformCrop(
            'net.mcreator.caerulaarbor.block.TentaclePlantBlock'),
        'planted_cell_crop': transformCrop(
            'net.mcreator.caerulaarbor.block.PlantedCellBlock'),
        'planted_fake_egg_crop': transformCrop(
            'net.mcreator.caerulaarbor.block.PlantedFakeEggBlock'),
        'plant_growth_age': transformProcedure(
            'net.mcreator.caerulaarbor.procedures.PlantGrowUpProcedure', 3),
        'can_continue_growth_age': transformProcedure(
            'net.mcreator.caerulaarbor.procedures.CanContinueToGrowProcedure', 1),
        'bone_boost_growth_age': transformProcedure(
            'net.mcreator.caerulaarbor.procedures.BoneBoostPlantProcedure', 2),
        'harvest_fake_egg_age': transformProcedure(
            'net.mcreator.caerulaarbor.procedures.HarvestFakeEggProcedure', 1)
    };
}

function transformCrop(className) {
    return {
        'target': {
            'type': 'CLASS',
            'name': className
        },
        'transformer': function(node) {
            if (node.superName != BLOCK) {
                throw new Error('Unexpected superclass for ' + className + ': ' + node.superName);
            }

            var constructorPatchedCount = 0;
            var agePropertyRenamedCount = 0;
            for (var methodIndex = 0; methodIndex < node.methods.size(); methodIndex++) {
                var method = node.methods.get(methodIndex);

                if (method.name == '<clinit>') {
                    for (var staticInstruction = method.instructions.getFirst();
                            staticInstruction != null;
                            staticInstruction = staticInstruction.getNext()) {
                        if (isAgePropertyInitializer(staticInstruction)) {
                            staticInstruction.cst = NEW_AGE_NAME;
                            agePropertyRenamedCount++;
                        }
                    }
                }

                if (method.name != '<init>') {
                    continue;
                }

                for (var instruction = method.instructions.getFirst(); instruction != null;
                        instruction = instruction.getNext()) {
                    if (instruction.getOpcode() == Opcodes.INVOKESPECIAL
                            && instruction.owner == BLOCK
                            && instruction.name == '<init>'
                            && instruction.desc == CONSTRUCTOR_DESC) {
                        instruction.owner = BRIDGE;
                        constructorPatchedCount++;
                    }
                }
            }

            if (constructorPatchedCount != 1) {
                throw new Error('Expected exactly one Block constructor call in ' + className
                        + ', found ' + constructorPatchedCount);
            }
            if (agePropertyRenamedCount != 1) {
                throw new Error('Expected exactly one blockstate property initializer in ' + className
                        + ', found ' + agePropertyRenamedCount);
            }
            node.superName = BRIDGE;
            return node;
        }
    };
}

function transformProcedure(className, expectedCount) {
    return {
        'target': {
            'type': 'CLASS',
            'name': className
        },
        'transformer': function(node) {
            var agePropertyRenamedCount = 0;
            for (var methodIndex = 0; methodIndex < node.methods.size(); methodIndex++) {
                var method = node.methods.get(methodIndex);
                if (method.name != 'execute') {
                    continue;
                }

                for (var instruction = method.instructions.getFirst(); instruction != null;
                        instruction = instruction.getNext()) {
                    if (isAgePropertyLookup(instruction)) {
                        instruction.cst = NEW_AGE_NAME;
                        agePropertyRenamedCount++;
                    }
                }
            }

            if (agePropertyRenamedCount != expectedCount) {
                throw new Error('Expected ' + expectedCount + ' blockstate property lookups in '
                        + className + '.execute, found ' + agePropertyRenamedCount);
            }
            return node;
        }
    };
}

function isAgePropertyInitializer(instruction) {
    if (instruction.getOpcode() != Opcodes.LDC
            || instruction.cst == null
            || String(instruction.cst) != OLD_AGE_NAME) {
        return false;
    }

    var minValue = nextOpcodeInstruction(instruction);
    var maxValue = nextOpcodeInstruction(minValue);
    var createCall = nextOpcodeInstruction(maxValue);
    return minValue != null
        && maxValue != null
        && createCall != null
        && minValue.getOpcode() == Opcodes.ICONST_0
        && maxValue.getOpcode() == Opcodes.ICONST_2
        && createCall.getOpcode() == Opcodes.INVOKESTATIC
        && createCall.owner == INTEGER_PROPERTY
        && createCall.name == INTEGER_PROPERTY_CREATE
        && createCall.desc == INTEGER_PROPERTY_CREATE_DESC;
}

function isAgePropertyLookup(instruction) {
    if (instruction.getOpcode() != Opcodes.LDC
            || instruction.cst == null
            || String(instruction.cst) != OLD_AGE_NAME) {
        return false;
    }

    var lookupCall = nextOpcodeInstruction(instruction);
    return lookupCall != null
        && lookupCall.getOpcode() == Opcodes.INVOKEVIRTUAL
        && lookupCall.owner == STATE_DEFINITION
        && lookupCall.name == GET_PROPERTY
        && lookupCall.desc == GET_PROPERTY_DESC;
}

function nextOpcodeInstruction(instruction) {
    if (instruction == null) {
        return null;
    }

    var next = instruction.getNext();
    while (next != null && next.getOpcode() < 0) {
        next = next.getNext();
    }
    return next;
}
