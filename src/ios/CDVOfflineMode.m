#import <Cordova/CDV.h>
#import "CDVOfflineMode.h"
#import "SBOfflineModeManager.h"

@implementation CDVOfflineMode

NSString *icb;

- (void)pluginInitialize {
    [[SBOfflineModeManager sharedManager] watchReachability];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(connectionStatusChanged:)
                                                 name: @"SBOfflineModeManagerConnectionStatusChanged"
                                               object:nil];
}


- (void)setInternalCallback:(CDVInvokedUrlCommand*)command
{
    icb = [command.arguments objectAtIndex:0];
    
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void)useCache:(CDVInvokedUrlCommand*)command
{
    BOOL useCache = [[command.arguments objectAtIndex:0] isEqualToString:@"true"];
    NSLog(@"[ios] use cache: %i", useCache);
    
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    
    [SBOfflineModeManager sharedManager].useCache = YES;
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void)setCheckConnectionURL:(CDVInvokedUrlCommand*)command
{
    NSString *url = [command.arguments objectAtIndex:0];
    NSLog(@"[ios] use URL: %@", url);
    
    [SBOfflineModeManager sharedManager].checkConnectionURL = url;
    
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}


- (void)connectionStatusChanged:(NSNotification*)notification {
    if(icb) {
        NSDictionary *dico = [NSDictionary
                              dictionaryWithObjectsAndKeys:
                              [NSNumber numberWithBool:[SBOfflineModeManager sharedManager].isOnline],
                              @"isOnline",
                              [NSNumber numberWithBool:[SBOfflineModeManager sharedManager].useCache],
                              @"usingCache",
                              nil
                              ];
        [self.commandDelegate
         evalJs:[NSString stringWithFormat:@"%@(%@)",
                 icb,
                 [self getJson:dico]
                 ]
         ];
    }
}


- (NSString *)getJson:(NSDictionary *)data {
    NSError *error;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:data
                                                       options:(NSJSONWritingOptions)NSJSONWritingPrettyPrinted
                                                         error:&error];
    if (!jsonData) {
        NSLog(@"getJson error: %@", error.localizedDescription);
        return @"{}";
    } else {
        return [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    }
}


@end
