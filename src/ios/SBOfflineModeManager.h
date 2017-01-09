#import <Foundation/Foundation.h>

const NSString *SBOfflineModeManagerConnectionStatusChanged = "SBOfflineModeManagerConnectionStatusChanged";

@interface SBOfflineModeManager : NSObject {    
    BOOL useCache;
    BOOL isOnline;
    
    NSString *checkConnectionURL;
}

@property (nonatomic, retain) NSString *checkConnectionURL;
@property (nonatomic, retain) BOOL *useCache;
@property (nonatomic, retain) BOOL *isOnline;

+ (id)sharedManager;
- (id)watchReachability;

@end
