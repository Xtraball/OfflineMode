#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>

@interface CDVOfflineMode : CDVPlugin

- (void)useCache:(CDVInvokedUrlCommand*)command;
- (void)pluginInitialize;

@end
