package com.bgsoftware.wildstacker.api;

import com.bgsoftware.wildstacker.api.handlers.DataManager;
import com.bgsoftware.wildstacker.api.handlers.SystemManager;

public interface WildStacker {

    SystemManager getSystemManager();

    DataManager getDataManager();

}