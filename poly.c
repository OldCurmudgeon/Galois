/*===================================================================*/
/* C program for distribution from the Combinatorial Object Server.  */
/* Program to enumerate all irreducible and/or primitive polynomials */
/* over GF(2).  This is the same version described in the book       */
/* "Combinatorial Generation", Frank Ruskey, to appear.              */
/* The program can be modified, translated to other languages, etc., */
/* so long as proper acknowledgement is given (author and source).   */
/* Not to be used for commercial purposes without prior consent.     */
/* The latest version of this program may be found at the site       */
/* http://www.theory.csc.uvic.ca/inf/neck/Polynomial.html       */
/* Copyright 1997,1998 F. Ruskey and K. Cattell                      */
/*===================================================================*/
                                                                            
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* Set MAX and ulong to the maximum number of bits in an unsigned integer  */
/* on your machine.   The settings below worked on a Sun Ulta 1, our test  */
/* machine.  You may need to change the typedef to "long" if your compiler */
/* doesn't support "long long".                                            */ 
typedef unsigned long long ulong;
#define MAX 64
#define ONE ((ulong)1)
#define TWO ((ulong)2)

/* globals */
int n;             /* degree of the polynomial */
ulong twonm1;      /* 2^n-1 */
ulong checked = 0; /* number checked so far */
ulong printed = 0; /* number printed so far */
ulong doneFlag;    /* for early exit */

/* run paramaters */
int type;
int outformat;
ulong maxNumber;
int printOrder;

/* stats */
ulong ir_count = 0, pr_count = 0; 
ulong prim_poly;

/* the necklace */
int b[MAX+1];

struct pair {
  ulong poly;      /* some primitive polynomial */
  ulong pow2m1;    /* 2^n - 1 */
};

struct pair poly_table[MAX+1] = {  /* prime factorization of 2^n-1 */
  /*  0 */  {  1,          0 },  /* (0)                               */
  /*  1 */  {  1,          1 },  /* (1)                               */
  /*  2 */  {  3,          3 },  /* (3)          it's prime!          */
  /*  3 */  {  3,          7 },  /* (7)          it's prime!          */
  /*  4 */  {  3,         15 },  /* (3) (5)                           */
  /*  5 */  {  5,         31 },  /* (31)         it's prime!          */
  /*  6 */  {  3,         63 },  /* (3) (3) (7)                       */
  /*  7 */  {  3,        127 },  /* (127)        it's prime!          */
  /*  8 */  { 29,        255 },  /* (3) (5) (17)                      */
  /*  9 */  { 17,        511 },  /* (7) (73)                          */
  /* 10 */  {  9,       1023 },  /* (3) (11) (31)                     */
  /* 11 */  {  5,       2047 },  /* (23) (89)                         */
  /* 12 */  { 83,       4095 },  /* (3) (3) (5) (7) (13)              */
  /* 13 */  { 27,       8191 },  /* (8191)       it's prime!          */
  /* 14 */  { 43,      16383 },  /* (3) (43) (127)                    */
  /* 15 */  {  3,      32767 },  /* (7) (31) (151)                    */
  /* 16 */  { 45,      65535 },  /* (3) (5) (17) (257)                */
  /* 17 */  {  9,     131071 },  /* (131071)     it's prime!          */
  /* 18 */  { 39,     262143 },  /* (3) (3) (7) (19) (73)             */
  /* 19 */  { 39,     524287 },  /* (524287)     it's prime!          */
  /* 20 */  {  9,    1048575 },  /* (3) (5) (5) (11) (31) (41)        */
  /* 21 */  {  5,    2097151 },  /* (7) (7) (127) (337)               */
  /* 22 */  {  3,    4194303 },  /* (3) (23) (89) (683)               */
  /* 23 */  { 33,    8388607 },  /* (47) (178481)                     */
  /* 24 */  { 27,   16777215 },  /* (3) (3) (5) (7) (13) (17) (241)   */
  /* 25 */  {  9,   33554431 },  /* (31) (601) (1801)                 */
  /* 26 */  { 71,   67108863 },  /* (3) (8191) (2731)                 */
  /* 27 */  { 39,  134217727 },  /* (7) (73) (262657)                 */
  /* 28 */  {  9,  268435455 },  /* (3) (5) (29) (43) (113) (127)     */
  /* 29 */  {  5,  536870911 },  /* (233) (1103) (2089)               */
  /* 30 */  { 83, 1073741823 },  /* (3) (3) (7) (11) (31) (151) (331) */
  /* 31 */  {  9, 2147483647 },  /* (2147483647) it's prime!          */
  /* 32 */  {175, 4294967295 }   /* (3) (5) (17) (257) (65537)        */
#if (MAX > 32) 
  ,
  /* 33 */  { 83, 8589934591 },  /* (7) (23) (89) (599479)            */
  /* 34 */  {231, 17179869183        },/*131071.3.43691*/
  /* 35 */  {5,   34359738367        },/*71.122921.31.127*/
  /* 36 */  {119, 68719476735        },/*7.73.3.3.19.13.5.37.109*/
  /* 37 */  {63,  137438953471       },/*616318177.223*/
  /* 38 */  {99,  274877906943       },/*524287.3.174763*/
  /* 39 */  {17,  549755813887       },/*7.8191.121369.79*/
  /* 40 */  {57,  1099511627775      },/*31.3.11.5.5.41.17.61681*/
  /* 41 */  {9,   2199023255551      },/*164511353.13367*/
  /* 42 */  {63,  4398046511103      },/*337.7.7.127.43.3.3.5419*/
  /* 43 */  {89,  8796093022207      },/*2099863.431.9719*/
  /* 44 */  {101, 17592186044415     },/*23.89.3.683.5.397.2113*/
  /* 45 */  {27,  35184372088831     },/*7.151.73.31.631.23311*/
  /* 46 */  {303, 70368744177663     },/*178481.47.3.2796203*/
  /* 47 */  {33,  140737488355327    },/*2351.13264529.4513*/
  /* 48 */  {183, 281474976710655    },/*7.3.3.13.5.17.241.257.97.673*/
  /* 49 */  {113, 562949953421311    },/*127.4432676798593*/
  /* 50 */  {29,  1125899906842623   },/*601.1801.31.3.11.251.4051*/
  /* 51 */  {75,  2251799813685247   },/*7.131071.11119.2143.103*/
  /* 52 */  {9,   4503599627370495   },/*8191.3.2731.5.53.157.1613*/
  /* 53 */  {71,  9007199254740991   },/*20394401.6361.69431*/
  /* 54 */  {125, 18014398509481983  },/*7.262657.73.19.3.3.3.3.87211*/
  /* 55 */  {71,  36028797018963967  },/*881.23.89.31.3191.201961*/
  /* 56 */  {149, 72057594037927935  },/*127.43.3.29.113.5.17.15790321*/
  /* 57 */  {45,  144115188075855871 },/*7.524287.1212847.32377*/
  /* 58 */  {99,  288230376151711743 },/*2089.233.1103.3.59.3033169*/
  /* 59 */  {123, 576460752303423487 },/*179951.3203431780337*/
  /* 60 */  { 3,  1152921504606846975},/*7.151.31.3.3.11.331.13.5.5.41.61.1321*/
  /* 61 */  {39,  2305843009213693951   },/*2305843009213693951*/
  /* 62 */  {105, 4611686018427387903   },/*2147483647.3.715827883*/
  /* 63,*/  {3 ,  9223372036854775807   },/*337.7.7.73.127.649657.92737*/
  /* 64 */  {27,  18446744073709551615  } /*3.5.17.257.65537.641.6700417*/
#endif
};

void ProcessInput (int argc, char *argv[]) { 
  outformat = 0;
  type = 0;
  maxNumber = 0;

  if (argc > 1 && argc <= 7) {
    n = atoi(argv[1]);
    if (argc > 2) outformat = atoi(argv[2]);
    if (argc > 3) type = atoi(argv[3]); 
    if (argc > 4) maxNumber = atoi(argv[4]);
    if (argc > 5) printOrder = atoi(argv[5]);
  } else {
    fprintf(stderr,"Usage: poly n format type number\n");
    fprintf(stderr,"         All params except 'n' are optional\n");
    fprintf(stderr,"         n = degree\n");
    fprintf(stderr,"         format = 0,1,2 = string,ceoffs,poly\n");
    fprintf(stderr,"         type = 0/1 = all/primitive only\n");
    fprintf(stderr,"         num = max to find (0 for all)\n");
    fprintf(stderr,"         ord = 1 = print order of poly\n");
    exit( 1 );
  }
  printf( "degree      = %d\n", n );
  printf( "format      = %d\n", outformat );
  printf( "type        = %s\n", type ? "primitive" : "all" );
  printf( "num         = %lld %s\n", maxNumber, maxNumber ? "" : "(none)" );
  printf( "ord         = %d %s\n", printOrder, printOrder ? "(yes)" : "(no)"  );
}

/* for checking primitivity */
ulong gcd ( ulong n, ulong m ) {
  if (m == 0) return(n); 
  else return( gcd( m, n%m ) );
}


void PrintBitString( int n, ulong a ) {
  int i;
  printf("1");  
  for ( i=n-1; i>0; i-- )
    if ( a & (ONE<<i) ) printf("1"); 
    else printf("0");
  printf("1\n");
}

void PrintPoly( int n, ulong a ) {
  int i;
  printf("x^%d + ", n );  
  for ( i=n-1; i>0; i-- )
    if ( a & (ONE<<i) ) {
      if (i != 1) printf("x^%d + ", i ); 
      else printf( "x + " );
    }
  printf("1\n"); 
}


void PrintCoefString( int n, ulong a ) {
  int i;
  printf( "%d\0", n );  
  for ( i=n-1; i>0; i-- )
    if ( a & (ONE<<i) ) printf( ", %d\0", i ); 
  printf(", 0\n"); 
}


ulong computeReverse ( int n, ulong a ) {
  int i;
  ulong a_rev = 1;
  for ( i=n-1; i>0; i-- )
     if ( a & (ONE<<i) ) a_rev |= (ONE << (n-i));     
  return a_rev;
}


ulong multmod( int n, ulong a, ulong b, ulong p ) {
  ulong t, rslt;
  ulong top_bit;
  int i;

  rslt = 0;
  t = a; /* t is a*x^i */
  top_bit = ONE << (n-1);

  for ( i=0; i<n; i++ ) {
     if (b & ONE) rslt ^= t;
     if (t & top_bit)
        t = ( (t & ~top_bit) << 1 ) ^ p;
     else
        t = t << 1;
     b = b >> 1;
  }
  return rslt;
}

ulong powmod( int n, ulong a, ulong power, ulong p ) {
  ulong t = a;
  ulong rslt = ONE;
  while ( power != 0 ) {
    if ( power & ONE ) rslt = multmod( n, t, rslt, p );
    t = multmod( n, t, t, p );
    power = power >> 1;
  }
  return rslt;
}

ulong minpoly( int n, ulong necklace, ulong p ) {
  ulong root, rslt = 0;
  ulong f[ MAX ];
  int i, j;

  f[0] = ONE;
  for (i=1; i<n; i++ ) f[i] = 0;

  root = powmod( n, TWO, necklace, p ); /* '2' is monomial x */
  for (i=1; i<=n; i++ ) {
    if (i != 1)
      root = multmod( n, root, root, p );

    for (j=n-1; j>=1; j--)
      f[j] = f[j-1] ^ multmod( n, f[j], root, p );
    f[0] = multmod( n, f[j], root, p );
  }

  for (i=0; i<n; i++ )
    if (f[i] == ONE)
      rslt |= ONE << i;
    else if (f[i] != 0)
      fprintf( stderr, "Ahh!" );

  return rslt;
}

ulong toInt ( ) {
ulong x;
int i;
  x = 0;
  for (i=1; i<=n; ++i) {
     x = TWO*x;
     if (b[i] == 1) ++x;
  }
  return x;
}

void OutputPolys( int n, ulong m, ulong gcdResult ) {
    if (printOrder) printf( "[%20llu] ", twonm1 / gcdResult ); 
    if (outformat == 0) PrintBitString( n, m ); 
    if (outformat == 1) PrintCoefString(n, m );
    if (outformat == 2) PrintPoly( n, m ); 
    printed++;
} 

void PrintIt( ulong p ) {
  ulong necklace;
  ulong m, m_rev;
  ulong gcdResult;
  int i;
  static int count = 0;

  if (p != n) return; 

  if (maxNumber && checked++ >= maxNumber) { 
    printf("Iteration limit exceeded !!\n");
    doneFlag = 1;
    return;
  }

  necklace = toInt();
  m = minpoly( n, necklace, prim_poly );

  /* check if it's primitive */
  gcdResult = gcd( necklace, twonm1 );
  m_rev = computeReverse( n, m );
  ++ir_count;
  if ( m != m_rev ) ++ir_count;

  if ( gcdResult == 1 ) {
    ++pr_count;
    OutputPolys( n, m, gcdResult );
    if ( m != m_rev ) {
       ++pr_count;
       OutputPolys( n, m_rev, gcdResult );
    }
  }

  /* print only if type = 0 (irreducible) */
  else if (type == 0) {
    OutputPolys( n, m, gcdResult );
    if ( m != m_rev ) 
       OutputPolys( n, m_rev, gcdResult );
  }
  if (maxNumber && printed >= maxNumber) {
    printf("Print limit exceeded !!\n");
    doneFlag = 1;
  }
}

void gen( int t, int p, int c ) {
  if (doneFlag) return;
  if (t > n) PrintIt( p );
  else {
    if (b[t-c] == 0) {
      if (b[t-p] == 0) {
        b[t] = 0;  gen( t+1, p, t );
      }
      b[t] = 1;
      if (b[t-p] == 1) gen( t+1, p, c );
      else gen( t+1, t, c );
    } else {
      b[t] = 0;  gen( t+1, p, c );
    }
  }
}


int main (int argc, char *argv[] ) {
  int i;
  ProcessInput( argc, argv );

  prim_poly = poly_table[n].poly;
  twonm1 = poly_table[n].pow2m1; 
  b[0] = b[1] = 0;
  gen( 2, 1, 1 );

  if (type == 0) {
    printf( "The number of irreducible polynomials = %llu\n", ir_count);
  }
  printf( "The number of primitive polynomials   = %llu\n", pr_count );
  return(0);
} 


