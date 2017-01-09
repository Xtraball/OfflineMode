#import <Foundation/Foundation.h>

NSString const *SBOfflineModeManagerConnectionStatusChanged = @"SBOfflineModeManagerConnectionStatusChanged";

@interface SBOfflineModeManager : NSObject {
    BOOL useCache;
    BOOL isOnline;
    
    NSString *checkConnectionURL;
}

@property (nonatomic, retain) NSString *checkConnectionURL;
@property (nonatomic) BOOL useCache;
@property (nonatomic) BOOL isOnline;

+ (SBOfflineModeManager *)sharedManager;
- (void)watchReachability;

@end
