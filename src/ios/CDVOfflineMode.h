#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>
#import "RNCachingURLProtocol.h"

@interface CDVOfflineMode : CDVPlugin

- (void)useCache:(CDVInvokedUrlCommand*)command;
- (void)pluginInitialize;

@end
