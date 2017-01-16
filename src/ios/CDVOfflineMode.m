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
    icb = command.callbackId;
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
                              nil
                              ];

        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                messageAsDictionary:dico];
        [result setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:result callbackId:icb];
    }
}

@end
