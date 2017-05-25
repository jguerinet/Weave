#!/usr/bin/env ts-node

/**************************************************************************************************
*
*       @file ./mobile-string-parser.ts
*       
*       Note: must be run from node - will not work if used in the browser.
*
*/

/******************************************** IMPORTS *********************************************/
import { spawnSync } from 'child_process';
import { readFileSync, unlinkSync } from 'fs';
import { wasRunAsScript } from 'mad-utils/lib/node';


/******************************************** LOGGING *********************************************/
import { buildFileTag, nodeLogFactory, colors } from 'mad-logs/lib/node';
const log = nodeLogFactory(buildFileTag('mobile-string-parser.ts', colors.bgMagenta.white));


/********************************************* EXPORT *********************************************/
/**
 * @export
 *
 * Get parsed string translations from googledoc. Erases old version, saves new downloaded & parsed
 * version as a JSON file, then grabs the file's data, converts it to a JS object & returns it.
 *
 * TODO try downloading first and save to temp file. Only delete if dwl succeeds. Copy new version
 *      in temp file to STRING_TRANSLATIONS.json.
 *
 * @return {Object} Translation strings object, parsed from STRING_TRANSLATIONS.json.
 */
export const getTranslationsFromWeb = (): Object => {
    try {
        unlinkSync('./STRING_TRANSLATIONS.json');
    } catch(e) {
        log.verbose(`STRING_TRANSLATIONS.json doesn't yet exist - no need to delete.`);
    }

    spawnSync('java', ['-jar', 'build/libs/mobile-string-parser-3.0.0.jar'], { env: process.env });

    const mobileStrings = require('./STRING_TRANSLATIONS.json');
    log.silly('mobileStrings returned from google translations repo:', mobileStrings);

    return mobileStrings;
};


/******************* RUN THE EXPORTED FUNCTION IF THE FILE WAS RUN AS A SCRIPT ********************/
if (wasRunAsScript(__filename)) getTranslationsFromWeb();
