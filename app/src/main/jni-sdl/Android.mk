LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
include $(firstword $(wildcard $(LOCAL_PATH)/../../build/sdl-extracted/SDL2-*/Android.mk))