/*
 * Copyright 2026 OmniOne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnione.did.mdoc.reader.di;

import org.omnione.did.mdoc.reader.config.ConfigProvider;
import org.omnione.did.mdoc.reader.settings.PreferencesManager;
import org.omnione.did.sdk.mdoc.proximity.reader.core.PlatformController;
import org.omnione.did.sdk.mdoc.proximity.reader.core.TransferController;

public interface DependencyProvider {
    ConfigProvider configProvider();
    PreferencesManager preferencesManager();
    PlatformController platformController();
    TransferController transferController();
}
