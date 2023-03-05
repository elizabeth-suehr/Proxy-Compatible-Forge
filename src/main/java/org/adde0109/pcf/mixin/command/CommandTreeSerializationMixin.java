package org.adde0109.pcf.mixin.command;

/*
The MIT License (MIT)

        Copyright (c) 2020 Andrew Steinborn
        Copyright (c) 2020 Velocity Contributors

        Permission is hereby granted, free of charge, to any person obtaining a copy
        of this software and associated documentation files (the "Software"), to deal
        in the Software without restriction, including without limitation the rights
        to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
        copies of the Software, and to permit persons to whom the Software is
        furnished to do so, subject to the following conditions:

        The above copyright notice and this permission notice shall be included in
        all copies or substantial portions of the Software.

        THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
        IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
        FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
        AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
        LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
        OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
        THE SOFTWARE.
*/

import com.mojang.brigadier.arguments.ArgumentType;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;



@Mixin(targets = "net.minecraft.network.protocol.game.ClientboundCommandsPacket$ArgumentNodeStub")
public class CommandTreeSerializationMixin {
  private static final int MOD_ARGUMENT_INDICATOR = -256;

  @Inject(method = "serializeCap(Lnet/minecraft/network/FriendlyByteBuf;Lnet/minecraft/commands/synchronization/ArgumentTypeInfo;Lnet/minecraft/commands/synchronization/ArgumentTypeInfo$Template;)V",
          at = @At("HEAD"), cancellable = true)
  private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void writeNode$wrapInVelocityModArgument(FriendlyByteBuf buf, ArgumentTypeInfo<A, T> serializer, ArgumentTypeInfo.Template<A> properties, CallbackInfo ci) {
    ResourceLocation identifier = Registry.COMMAND_ARGUMENT_TYPE.getKey(serializer);

    if (identifier == null) {
      return;
    }
    if (identifier.getNamespace().equals("minecraft") || identifier.getNamespace().equals("brigadier")) {
      return;
    }
    ci.cancel();

    // Not a standard Minecraft argument type - so we need to wrap it
    serializeWrappedArgumentType(buf, serializer, properties);
  }

  private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeWrappedArgumentType(FriendlyByteBuf packetByteBuf, ArgumentTypeInfo<A, T> serializer, ArgumentTypeInfo.Template<A> properties) {
    packetByteBuf.writeVarInt(MOD_ARGUMENT_INDICATOR);
    packetByteBuf.writeVarInt(Registry.COMMAND_ARGUMENT_TYPE.getId(serializer));

    FriendlyByteBuf extraData = new FriendlyByteBuf(Unpooled.buffer());
    serializer.serializeToNetwork((T) properties, extraData);

    packetByteBuf.writeVarInt(extraData.readableBytes());
    packetByteBuf.writeBytes(extraData);
  }
}
