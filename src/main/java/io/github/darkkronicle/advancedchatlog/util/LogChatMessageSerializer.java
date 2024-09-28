/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatlog.util;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import io.github.darkkronicle.advancedchatcore.chat.ChatMessage;
import io.github.darkkronicle.advancedchatcore.interfaces.IJsonSave;
import io.github.darkkronicle.advancedchatlog.config.ChatLogConfigStorage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.registry.*;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

@Environment(EnvType.CLIENT)
public class LogChatMessageSerializer implements IJsonSave<LogChatMessage> {

    final RegistryWrapper.WrapperLookup wrapperLookup = BuiltinRegistries.createWrapperLookup();

    private DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public LogChatMessageSerializer() {}

    private Style cleanStyle(Style style) {
        if (!ChatLogConfigStorage.General.CLEAN_SAVE.config.getBooleanValue()) {
            return style;
        }
        style = style.withClickEvent(null);
        style = style.withHoverEvent(null);
        style = style.withInsertion(null);
        return style;
    }

    private Text transfer(Text text) {
        // Using the built in serializer LiteralText is required
        Text base = Text.empty();
        for (Text t : text.getSiblings()) {
            Text newT = Text.literal(t.getString()).fillStyle(cleanStyle(t.getStyle()));
            base.getSiblings().add(newT);
        }
        return base;
    }

    @Override
    public LogChatMessage load(JsonObject obj) {
        LocalDateTime dateTime = LocalDateTime.from(formatter.parse(obj.get("time").getAsString()));
        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();

        Text display = fromJson(obj.get("display"), wrapperLookup);

        Text original = fromJson(obj.get("original"), wrapperLookup);

        ChatMessage message =
                ChatMessage.builder()
                        .time(time)
                        .displayText(display)
                        .originalText(original)
                        .build();
        LogChatMessage log = new LogChatMessage(message, date);
        return log;
    }

    @Override
    public JsonObject save(LogChatMessage message) {
        JsonObject json = new JsonObject();
        ChatMessage chat = message.getMessage();
        LocalDateTime dateTime = LocalDateTime.of(message.getDate(), chat.getTime());
        json.addProperty("time", formatter.format(dateTime));
        json.addProperty("stacks", chat.getStacks());
        json.add("display", toJson(transfer(chat.getDisplayText()), wrapperLookup));
        json.add("original", toJson(transfer(chat.getOriginalText()), wrapperLookup));
        return json;
    }

    static JsonElement toJson(Text text, RegistryWrapper.WrapperLookup registries) {
        return (JsonElement)TextCodecs.CODEC.encodeStart(registries.getOps(JsonOps.INSTANCE), text).getOrThrow(JsonParseException::new);
    }

    static MutableText fromJson(JsonElement json, RegistryWrapper.WrapperLookup registries) {
        return (MutableText)TextCodecs.CODEC.parse(registries.getOps(JsonOps.INSTANCE), json).getOrThrow(JsonParseException::new);
    }
}
