#!/usr/bin/env node

const { spawnSync } = require('child_process');
const { readFileSync, unlinkSync } = require('fs'); 

function getTranslationsFromWeb() {
    // unlinkSync('./STRING_TRANSLATIONS.json');

    spawnSync('java', ['-jar', 'build/libs/mobile-string-parser-3.0.0.jar'], { env: process.env });

    const mobileStrings = require('./STRING_TRANSLATIONS.json');

    return mobileStrings;
}

