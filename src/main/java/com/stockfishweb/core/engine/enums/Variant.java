/* Copyright 2018 David Cai Wang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stockfishweb.core.engine.enums;

/**
 * Variant of Stockfish process.
 *
 * @author Niflheim
 * @since 1.0
 */
public enum Variant {
    /**
     * Works on Unix and Windows machines
     */
    DEFAULT,
    /**
     * Works on Unix and Windows machines
     */
    BMI2,
    /**
     * Works on Windows machines
     */
    POPCNT,
    /**
     * Works on Unix machines
     */
    MODERN
}
