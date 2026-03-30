//
//  mdocReaderApp.swift
//  mdocReader
//
//  Created by sjkim on 3/26/26.
//

import SwiftUI

@main
struct mdocReaderApp: App {
    var body: some Scene {
        WindowGroup {
            NavigationView {
                ReaderView()
                    .navigationTitle("mDoc Reader")
            }
        }
    }
}
