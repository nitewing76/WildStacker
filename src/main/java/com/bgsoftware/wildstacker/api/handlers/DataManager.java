package com.bgsoftware.wildstacker.api.handlers;

import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import com.bgsoftware.wildstacker.api.objects.StackedSpawner;

public interface DataManager {

    void addStackedSpawner(StackedSpawner stackedSpawner);

    void removeStackedSpawner(StackedSpawner stackedSpawner);

    void addStackedBarrel(StackedBarrel stackedBarrel);

    void removeStackedBarrel(StackedBarrel stackedBarrel);

}
