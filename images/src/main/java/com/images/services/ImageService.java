package com.images.services;

import java.awt.image.BufferedImage;

public interface ImageService {
    public boolean imageContainsCat(BufferedImage image, float confidenceThreshhold);
}
