/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.model.modpack;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.net.URL;

@Data
@EqualsAndHashCode(callSuper = true)
public class ManifestInfo extends BaseManifest implements Comparable<ManifestInfo> {

    private String location;
    private int priority;
    @Getter private URL iconUrl;
    @Getter private String domainName;
    @Getter private URL newsUrl;

    @Override
    public int compareTo(ManifestInfo o) {
        if (priority > o.getPriority()) {
            return -1;
        } else if (priority < o.getPriority()) {
            return 1;
        } else {
            return 0;
        }
    }

}
