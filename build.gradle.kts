// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
	repositories {
		jcenter()
        google()
        mavenCentral()
    }
	dependencies  {
	   classpath(libs.gradle)
	   classpath(libs.kotlin.gradle.plugin)
	}
}

allprojects {
    repositories {
        jcenter()
        google()
    }
}

// List of plugins used in the app, but do not apply for the root project
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}