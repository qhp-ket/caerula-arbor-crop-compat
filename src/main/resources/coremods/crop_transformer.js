var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');

var BLOCK = 'net/minecraft/world/level/block/Block';
var BRIDGE = 'io/github/caerulacropcompat/CaerulaCropBlock';
var CONSTRUCTOR_DESC = '(Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V';
var HWE_HARVEST_UTILS = 'it.crystalnest.harvest_with_ease.api.HarvestUtils';
var HWE_GET_AGE_DESC =
    '(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/properties/IntegerProperty;';
var BLOCK_STATE = 'net/minecraft/world/level/block/state/BlockState';
var BLOCK_STATE_GET_BLOCK = 'm_60734_';
var BLOCK_STATE_GET_BLOCK_DESC = '()Lnet/minecraft/world/level/block/Block;';
var BRIDGE_AGE_HELPER = 'getCompatibilityAgeProperty';
var BRIDGE_AGE_HELPER_DESC = '()Lnet/minecraft/world/level/block/state/properties/IntegerProperty;';

function initializeCoreMod() {
    var transformers = {
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
            'net.mcreator.caerulaarbor.block.PlantedFakeEggBlock')
    };
    // Missing CLASS targets are never visited by ModLauncher. Always declaring
    // this optional target avoids a too-early Java.type() availability check.
    transformers['harvest_with_ease_caerula_age'] = transformHarvestWithEaseAge();
    return transformers;
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
            for (var methodIndex = 0; methodIndex < node.methods.size(); methodIndex++) {
                var method = node.methods.get(methodIndex);

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
            node.superName = BRIDGE;
            return node;
        }
    };
}

function transformHarvestWithEaseAge() {
    return {
        'target': {
            'type': 'CLASS',
            'name': HWE_HARVEST_UTILS
        },
        'transformer': function(node) {
            var target = null;
            var count = 0;
            for (var methodIndex = 0; methodIndex < node.methods.size(); methodIndex++) {
                var method = node.methods.get(methodIndex);
                var isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
                if (method.name == 'getAge' && method.desc == HWE_GET_AGE_DESC && isStatic) {
                    target = method;
                    count++;
                }
            }
            if (count != 1) {
                ASMAPI.log('WARN', '[Caerula Crop Compat] Harvest With Ease compatibility was not '
                        + 'applied: expected exactly one static getAge(BlockState), found ' + count + '.');
                return node;
            }

            var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
            var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
            var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
            var TypeInsnNode = Java.type('org.objectweb.asm.tree.TypeInsnNode');
            var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
            var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
            var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
            var continueOriginal = new LabelNode();
            var patch = new InsnList();

            patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
            patch.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, BLOCK_STATE,
                    BLOCK_STATE_GET_BLOCK, BLOCK_STATE_GET_BLOCK_DESC, false));
            patch.add(new TypeInsnNode(Opcodes.INSTANCEOF, BRIDGE));
            patch.add(new JumpInsnNode(Opcodes.IFEQ, continueOriginal));
            patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
            patch.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, BLOCK_STATE,
                    BLOCK_STATE_GET_BLOCK, BLOCK_STATE_GET_BLOCK_DESC, false));
            patch.add(new TypeInsnNode(Opcodes.CHECKCAST, BRIDGE));
            patch.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, BRIDGE,
                    BRIDGE_AGE_HELPER, BRIDGE_AGE_HELPER_DESC, false));
            patch.add(new InsnNode(Opcodes.ARETURN));
            patch.add(continueOriginal);
            target.instructions.insertBefore(target.instructions.getFirst(), patch);
            ASMAPI.log('INFO', '[Caerula Crop Compat] Applied optional Harvest With Ease '
                    + 'blockstate age-property patch to HarvestUtils#getAge(BlockState).');
            return node;
        }
    };
}
